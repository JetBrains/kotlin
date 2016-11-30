/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.common

import kotlin.reflect.*
import kotlin.reflect.jvm.jvmErasure

fun tryConstructClassFromStringArgs(scriptClass: Class<*>, scriptArgs: List<String>): Any? {

    try {
        return scriptClass.getConstructor(Array<String>::class.java).newInstance(scriptArgs.toTypedArray())
    }
    catch (e: NoSuchMethodException) {
        for (ctor in scriptClass.kotlin.constructors) {
            val mapping = tryCreateCallableMapping(ctor, scriptArgs, StringArgsConverter())
            if (mapping != null) {
                try {
                    return ctor.callBy(mapping)
                }
                catch (e: Exception) { // TODO: find the exact exception type thrown then callBy fails
                }
            }
        }
    }
    return null
}


fun tryCreateCallableMapping(callable: KCallable<*>, args: List<Any?>): Map<KParameter, Any?>? =
        tryCreateCallableMapping(callable, args, AnyArgsConverter())


private interface ArgsConverter<T> {

    data class Result(val v: Any?, val argsConsumed: Int)

    fun convert(parameter: KParameter, args: List<T>, startArgIndex: Int): Result?
}

private fun <T: Any?> tryCreateCallableMapping(callable: KCallable<*>, args: List<T>, converter: ArgsConverter<T>): Map<KParameter, Any?>? {

    var argIdx = 0
    val res = mutableMapOf<KParameter, Any?>()
    for (par in callable.parameters) {
        val (compatibleArg, argsConsumed) = converter.convert(par, args, argIdx) ?: return null
        if (argsConsumed > 0) {
            res.put(par, compatibleArg)
            argIdx += argsConsumed
        }
    }
    return res
}

private class StringArgsConverter : ArgsConverter<String> {

    override fun convert(parameter: KParameter, args: List<String>, startArgIndex: Int): ArgsConverter.Result? {

        fun convertPrimitive(type: KType?, arg: String): Any? =
                when (type?.classifier) {
                    String::class -> arg
                    Int::class -> arg.toInt()
                    Long::class -> arg.toLong()
                    Short::class -> arg.toShort()
                    Byte::class -> arg.toByte()
                    Char::class -> arg[0]
                    Float::class -> arg.toFloat()
                    Double::class -> arg.toDouble()
                    Boolean::class -> arg.toBoolean()
                    else -> null
                }

        fun convertArray(type: KType?, args: List<String>): Any? =
                when (type?.classifier) {
                    String::class -> args.toTypedArray()
                    Int::class -> args.map(String::toInt).toTypedArray()
                    Long::class -> args.map(String::toLong).toTypedArray()
                    Short::class -> args.map(String::toShort).toTypedArray()
                    Byte::class -> args.map(String::toByte).toTypedArray()
                    Char::class -> args.map { it[0] }.toTypedArray()
                    Float::class -> args.map(String::toFloat).toTypedArray()
                    Double::class -> args.map(String::toDouble).toTypedArray()
                    Boolean::class -> args.map(String::toBoolean).toTypedArray()
                    else -> null
                }

        try {
            if (startArgIndex >= args.size && parameter.isOptional)
                return ArgsConverter.Result(null, 0)

            if (startArgIndex < args.size) {
                val primArgCandidate = convertPrimitive(parameter.type, args[startArgIndex])
                if (primArgCandidate != null)
                    return ArgsConverter.Result(primArgCandidate, 1)
            }

            if ((parameter.type.classifier as? KClass<*>)?.qualifiedName == Array<Any>::class.qualifiedName) {
                val arrCompType = parameter.type.arguments.getOrNull(0)?.type
                val arrayArgCandidate = convertArray(arrCompType, args.drop(startArgIndex))
                if (arrayArgCandidate != null)
                    return ArgsConverter.Result(arrayArgCandidate, args.size - startArgIndex)
            }
        }
        catch (e: NumberFormatException) {}

        return null
    }
}

private class AnyArgsConverter : ArgsConverter<Any?> {
    override fun convert(parameter: KParameter, args: List<Any?>, startArgIndex: Int): ArgsConverter.Result? {

        fun convertSingle(type: KType, arg: Any?): Any? = when {
            type.classifier == arg?.javaClass?.kotlin -> arg
            type.jvmErasure.java.isAssignableFrom(arg?.javaClass) -> arg
            else -> null
        }

        if (startArgIndex >= args.size && parameter.isOptional)
            return ArgsConverter.Result(null, 0)

        if (startArgIndex < args.size) {
            val primArgCandidate = convertSingle(parameter.type, args[startArgIndex])
            if (primArgCandidate != null)
                return ArgsConverter.Result(primArgCandidate, 1)
        }
        // TODO: add vararg support
        return null
    }
}

