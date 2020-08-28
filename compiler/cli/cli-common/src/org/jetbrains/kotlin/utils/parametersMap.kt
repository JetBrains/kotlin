/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import kotlin.reflect.*
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

fun tryConstructClassFromStringArgs(clazz: Class<*>, args: List<String>): Any? {
    return try {
        clazz.getConstructor(Array<String>::class.java).newInstance(args.toTypedArray())
    } catch (e: NoSuchMethodException) {
        for (ctor in clazz.kotlin.constructors) {
            val mapping = tryCreateCallableMappingFromStringArgs(ctor, args)
            if (mapping != null) {
                try {
                    return ctor.callBy(mapping)
                } catch (e: Exception) { // TODO: find the exact exception type thrown then callBy fails
                }
            }
        }
        null
    }
}

fun tryCreateCallableMapping(callable: KCallable<*>, args: List<Any?>): Map<KParameter, Any?>? =
    tryCreateCallableMapping(
        callable,
        args.map { NamedArgument(null, it) }.iterator(),
        AnyArgsConverter()
    )

fun tryCreateCallableMappingFromStringArgs(callable: KCallable<*>, args: List<String>): Map<KParameter, Any?>? =
    tryCreateCallableMapping(
        callable,
        args.map { NamedArgument(null, it) }.iterator(),
        StringArgsConverter()
    )

fun tryCreateCallableMappingFromNamedArgs(callable: KCallable<*>, args: List<Pair<String?, Any?>>): Map<KParameter, Any?>? =
    tryCreateCallableMapping(
        callable,
        args.map {
            NamedArgument(
                it.first,
                it.second
            )
        }.iterator(),
        AnyArgsConverter()
    )

// ------------------------------------------------

private data class NamedArgument<out T>(val name: String?, val value: T?)

private interface ArgsConverter<T> {
    sealed class Result {
        object Failure : Result()
        class Success(val v: Any?) : Result()
    }

    fun tryConvertSingle(parameter: KParameter, arg: NamedArgument<T>): Result
    fun tryConvertVararg(parameter: KParameter, firstArg: NamedArgument<T>, restArgs: Sequence<NamedArgument<T>>): Result
    fun tryConvertTail(parameter: KParameter, firstArg: NamedArgument<T>, restArgs: Sequence<NamedArgument<T>>): Result
}

private enum class ArgsTraversalState { UNNAMED, NAMED, TAIL }

private fun <T> tryCreateCallableMapping(
    callable: KCallable<*>,
    args: Iterator<NamedArgument<T>>,
    converter: ArgsConverter<T>
): Map<KParameter, Any?>? {
    val res = mutableMapOf<KParameter, Any?>()
    var state = ArgsTraversalState.UNNAMED
    val unboundParams = callable.parameters.toMutableList()
    val argIt = LookAheadIterator(args.iterator())
    while (argIt.hasNext()) {
        if (unboundParams.isEmpty()) return null // failed to match: no param left for the arg
        val arg = argIt.next()
        when (state) {
            ArgsTraversalState.UNNAMED -> if (arg.name != null) state =
                ArgsTraversalState.NAMED
            ArgsTraversalState.NAMED -> if (arg.name == null) state =
                ArgsTraversalState.TAIL
            ArgsTraversalState.TAIL -> if (arg.name != null) throw IllegalArgumentException("Illegal mix of named and unnamed arguments")
        }
        // TODO: check the logic of named/unnamed/tail(vararg or lambda) arguments matching
        when (state) {
            ArgsTraversalState.UNNAMED -> {
                val par = unboundParams.removeAt(0)
                // try single argument first
                val cvtRes = converter.tryConvertSingle(par, arg)
                if (cvtRes is ArgsConverter.Result.Success) {
                    if (cvtRes.v == null && !par.type.allowsNulls()) {
                        // if we do not allow to overload on nullability, drop this check
                        return null // failed to match: null for a non-nullable value
                    }
                    res[par] = cvtRes.v
                } else if (par.type.jvmErasure.java.isArray) {
                    // try vararg

                    // Collect all the arguments that do not have a name
                    val unnamed = argIt.sequenceUntil { it.name != null }

                    val cvtVRes = converter.tryConvertVararg(par, arg, unnamed)
                    if (cvtVRes is ArgsConverter.Result.Success) {
                        res[par] = cvtVRes.v
                    } else return null // failed to match: no suitable param for unnamed arg
                } else return null // failed to match: no suitable param for unnamed arg
            }
            ArgsTraversalState.NAMED -> {
                assert(arg.name != null)
                val parIdx = unboundParams.indexOfFirst { it.name == arg.name }.takeIf { it >= 0 }
                    ?: return null // failed to match: no matching named parameter found
                val par = unboundParams.removeAt(parIdx)
                val cvtRes = converter.tryConvertSingle(par, arg)
                if (cvtRes is ArgsConverter.Result.Success) {
                    res[par] = cvtRes.v
                } else return null // failed to match: cannot convert arg to param's type
            }
            ArgsTraversalState.TAIL -> {
                assert(arg.name == null)
                val par = unboundParams.removeAt(unboundParams.lastIndex)
                val cvtVRes = converter.tryConvertTail(par, arg, argIt.asSequence())
                if (cvtVRes is ArgsConverter.Result.Success) {
                    if (argIt.hasNext()) return null // failed to match: not all tail args are consumed
                    res[par] = cvtVRes.v
                } else return null // failed to match: no suitable param for tail arg(s)
            }
        }
    }
    return when {
        unboundParams.any { !it.isOptional && !it.isVararg } -> null // fail to match: non-optional params remained
        else -> res
    }
}

private fun KType.allowsNulls(): Boolean =
    isMarkedNullable || classifier.let { it is KTypeParameter && it.upperBounds.any(KType::allowsNulls) }


private class StringArgsConverter : ArgsConverter<String> {
    override fun tryConvertSingle(parameter: KParameter, arg: NamedArgument<String>): ArgsConverter.Result {
        val value = arg.value ?: return ArgsConverter.Result.Success(null)

        val primitive: Any? = when (parameter.type.classifier) {
            String::class -> value
            Int::class -> value.toIntOrNull()
            Long::class -> value.toLongOrNull()
            Short::class -> value.toShortOrNull()
            Byte::class -> value.toByteOrNull()
            Char::class -> value.singleOrNull()
            Float::class -> value.toFloatOrNull()
            Double::class -> value.toDoubleOrNull()
            Boolean::class -> value.toBoolean()
            else -> null
        }

        return if (primitive != null) ArgsConverter.Result.Success(primitive) else ArgsConverter.Result.Failure
    }

    override fun tryConvertVararg(
        parameter: KParameter,
        firstArg: NamedArgument<String>,
        restArgs: Sequence<NamedArgument<String>>
    ): ArgsConverter.Result {
        fun convertPrimitivesArray(type: KType, args: Sequence<String?>): Any? =
            when (type.classifier) {
                IntArray::class -> args.map { it?.toIntOrNull() }
                LongArray::class -> args.map { it?.toLongOrNull() }
                ShortArray::class -> args.map { it?.toShortOrNull() }
                ByteArray::class -> args.map { it?.toByteOrNull() }
                CharArray::class -> args.map { it?.singleOrNull() }
                FloatArray::class -> args.map { it?.toFloatOrNull() }
                DoubleArray::class -> args.map { it?.toDoubleOrNull() }
                BooleanArray::class -> args.map { it?.toBoolean() }
                else -> null
            }?.toList()?.takeUnless { null in it }?.toTypedArray()

        val parameterType = parameter.type
        if (parameterType.jvmErasure.java.isArray) {
            val argsSequence = sequenceOf(firstArg.value) + restArgs.map { it.value }
            val primArrayArgCandidate = convertPrimitivesArray(parameterType, argsSequence)
            if (primArrayArgCandidate != null)
                return ArgsConverter.Result.Success(primArrayArgCandidate)
            val arrayElementType = parameterType.arguments.firstOrNull()?.type
            val arrayArgCandidate = convertAnyArray(arrayElementType?.classifier, argsSequence)
            if (arrayArgCandidate != null)
                return ArgsConverter.Result.Success(arrayArgCandidate)
        }

        return ArgsConverter.Result.Failure
    }

    override fun tryConvertTail(
        parameter: KParameter,
        firstArg: NamedArgument<String>,
        restArgs: Sequence<NamedArgument<String>>
    ): ArgsConverter.Result =
        tryConvertVararg(parameter, firstArg, restArgs)
}

private class AnyArgsConverter : ArgsConverter<Any> {
    override fun tryConvertSingle(parameter: KParameter, arg: NamedArgument<Any>): ArgsConverter.Result {
        val value = arg.value ?: return ArgsConverter.Result.Success(null)

        @Suppress("UNCHECKED_CAST")
        fun convertPrimitivesArray(type: KType?, arg: Any?): Any? =
            when (type?.classifier) {
                IntArray::class -> (arg as? Array<Int>)?.toIntArray()
                LongArray::class -> (arg as? Array<Long>)?.toLongArray()
                ShortArray::class -> (arg as? Array<Short>)?.toShortArray()
                ByteArray::class -> (arg as? Array<Byte>)?.toByteArray()
                CharArray::class -> (arg as? Array<Char>)?.toCharArray()
                FloatArray::class -> (arg as? Array<Float>)?.toFloatArray()
                DoubleArray::class -> (arg as? Array<Double>)?.toDoubleArray()
                BooleanArray::class -> (arg as? Array<Boolean>)?.toBooleanArray()
                else -> null
            }

        fun evaluateValue(arg: Any): Any? {
            if (arg::class.isSubclassOf(parameter.type.jvmErasure)) return arg
            return convertPrimitivesArray(parameter.type, arg)
        }

        evaluateValue(value)?.let { return ArgsConverter.Result.Success(it) }

        // Handle the scenario where [arg::class] is an Array<Any>
        // but it's values could all still be valid
        val parameterKClass = parameter.type.classifier as? KClass<*>
        val arrayComponentType = parameterKClass?.java?.takeIf { it.isArray}?.componentType?.kotlin

        if (value is Array<*> && arrayComponentType != null) {
            // TODO: Idea! Maybe we should check if the values in the array are compatible with [arrayComponentType]
            //  if they aren't perhaps we should fail silently
            convertAnyArray(arrayComponentType, value.asSequence())?.let(::evaluateValue)?.let { return ArgsConverter.Result.Success(it) }
        }

        return ArgsConverter.Result.Failure
    }

    override fun tryConvertVararg(
        parameter: KParameter, firstArg: NamedArgument<Any>, restArgs: Sequence<NamedArgument<Any>>
    ): ArgsConverter.Result {
        val parameterType = parameter.type
        if (parameterType.jvmErasure.java.isArray) {
            val argsSequence = sequenceOf(firstArg.value) + restArgs.map { it.value }
            val arrayElementType = parameterType.arguments.firstOrNull()?.type
            val arrayArgCandidate = convertAnyArray(arrayElementType?.classifier, argsSequence)
            if (arrayArgCandidate != null)
                return ArgsConverter.Result.Success(arrayArgCandidate)
        }

        return ArgsConverter.Result.Failure
    }

    override fun tryConvertTail(
        parameter: KParameter,
        firstArg: NamedArgument<Any>,
        restArgs: Sequence<NamedArgument<Any>>
    ): ArgsConverter.Result =
        tryConvertSingle(parameter, firstArg)
}

@Suppress("UNCHECKED_CAST")
private inline fun <reified T> convertAnyArray(classifier: KClassifier?, args: Sequence<T?>): Any? =
    if (classifier == T::class) args.toList().toTypedArray() // simple case
    else convertAnyArrayImpl<T>(classifier, args)

private fun <T> convertAnyArrayImpl(classifier: KClassifier?, args: Sequence<T?>): Any? {
    val elementClass = (classifier as? KClass<*>) ?: return null

    val argsList = args.toList()
    val result = java.lang.reflect.Array.newInstance(elementClass.java, argsList.size)
    argsList.forEachIndexed { idx, arg ->
        try {
            java.lang.reflect.Array.set(result, idx, arg)
        } catch (e: IllegalArgumentException) {
            return@convertAnyArrayImpl null
        }
    }
    return result
}

/*
 An iterator that allows us to read the next value without consuming it.
 */
private class LookAheadIterator<T>(private val iterator: Iterator<T>) : Iterator<T> {
    private var currentLookAhead: T? = null

    override fun hasNext(): Boolean {
        return currentLookAhead != null || iterator.hasNext()
    }

    override fun next(): T {
        currentLookAhead?.let { value ->
            currentLookAhead = null
            return value
        }

        return iterator.next()
    }

    fun nextWithoutConsuming(): T {
        return currentLookAhead ?: iterator.next().also { currentLookAhead = it }
    }
}

/*
 Will return a sequence with the values of the iterator until the predicate evaluates to true.
 */
private fun <T> LookAheadIterator<T>.sequenceUntil(predicate: (T) -> Boolean): Sequence<T> = sequence {
    while (hasNext()) {
        if (predicate(nextWithoutConsuming()))
            break
        yield(next())
    }
}
