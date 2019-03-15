/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.calls

import java.lang.reflect.Proxy
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.internal.structure.wrapperByPrimitive
import java.lang.reflect.Method as ReflectMethod

internal class AnnotationConstructorCaller(
    private val jClass: Class<*>,
    private val parameterNames: List<String>,
    private val callMode: CallMode,
    origin: Origin,
    private val methods: List<ReflectMethod> = parameterNames.map { name -> jClass.getDeclaredMethod(name) }
) : Caller<Nothing?> {
    override val member: Nothing?
        get() = null

    override val returnType: Type
        get() = jClass

    override val parameterTypes: List<Type> = methods.map { it.genericReturnType }

    enum class CallMode { CALL_BY_NAME, POSITIONAL_CALL }

    enum class Origin { JAVA, KOTLIN }

    // Transform primitive int to java.lang.Integer because actual arguments passed here will be boxed and Class#isInstance should succeed
    private val erasedParameterTypes: List<Class<*>> = methods.map { method -> method.returnType.let { it.wrapperByPrimitive ?: it } }

    private val defaultValues: List<Any?> = methods.map { method -> method.defaultValue }

    init {
        // TODO: consider lifting this restriction once KT-8957 is implemented
        if (callMode == CallMode.POSITIONAL_CALL && origin == Origin.JAVA && (parameterNames - "value").isNotEmpty()) {
            throw UnsupportedOperationException(
                "Positional call of a Java annotation constructor is allowed only if there are no parameters " +
                        "or one parameter named \"value\". This restriction exists because Java annotations (in contrast to Kotlin)" +
                        "do not impose any order on their arguments. Use KCallable#callBy instead."
            )
        }
    }

    override fun call(args: Array<*>): Any? {
        checkArguments(args)

        val values = args.mapIndexed { index, arg ->
            val value =
                if (arg == null && callMode == CallMode.CALL_BY_NAME) defaultValues[index]
                else arg.transformKotlinToJvm(erasedParameterTypes[index])
            value ?: throwIllegalArgumentType(index, parameterNames[index], erasedParameterTypes[index])
        }

        return createAnnotationInstance(jClass, parameterNames.zip(values).toMap(), methods)
    }
}

/**
 * Transforms a Kotlin value to the one required by the JVM, e.g. KClass<*> -> Class<*> or Array<KClass<*>> -> Array<Class<*>>.
 * Returns `null` in case when no transformation is possible (an argument of an incorrect type was passed).
 */
private fun Any?.transformKotlinToJvm(expectedType: Class<*>): Any? {
    @Suppress("UNCHECKED_CAST")
    val result = when (this) {
        is Class<*> -> return null
        is KClass<*> -> this.java
        is Array<*> -> when {
            this.isArrayOf<Class<*>>() -> return null
            this.isArrayOf<KClass<*>>() -> (this as Array<KClass<*>>).map(KClass<*>::java).toTypedArray()
            else -> this
        }
        else -> this
    }

    return if (expectedType.isInstance(result)) result else null
}

private fun throwIllegalArgumentType(index: Int, name: String, expectedJvmType: Class<*>): Nothing {
    val kotlinClass = when {
        expectedJvmType == Class::class.java -> KClass::class
        expectedJvmType.isArray && expectedJvmType.componentType == Class::class.java ->
            @Suppress("CLASS_LITERAL_LHS_NOT_A_CLASS") Array<KClass<*>>::class // Workaround KT-13924
        else -> expectedJvmType.kotlin
    }
    // For arrays, also render the type argument in the message, e.g. "... not of the required type kotlin.Array<kotlin.reflect.KClass>"
    val typeString = when {
        kotlinClass.qualifiedName == Array<Any>::class.qualifiedName ->
            "${kotlinClass.qualifiedName}<${kotlinClass.java.componentType.kotlin.qualifiedName}>"
        else -> kotlinClass.qualifiedName
    }
    throw IllegalArgumentException("Argument #$index $name is not of the required type $typeString")
}

internal fun <T : Any> createAnnotationInstance(
    annotationClass: Class<T>,
    values: Map<String, Any>,
    methods: List<ReflectMethod> = values.keys.map { name -> annotationClass.getDeclaredMethod(name) }
): T {
    fun equals(other: Any?): Boolean =
        (other as? Annotation)?.annotationClass?.java == annotationClass &&
                methods.all { method ->
                    val ours = values[method.name]
                    val theirs = method(other)
                    when (ours) {
                        is BooleanArray -> Arrays.equals(ours, theirs as BooleanArray)
                        is CharArray -> Arrays.equals(ours, theirs as CharArray)
                        is ByteArray -> Arrays.equals(ours, theirs as ByteArray)
                        is ShortArray -> Arrays.equals(ours, theirs as ShortArray)
                        is IntArray -> Arrays.equals(ours, theirs as IntArray)
                        is FloatArray -> Arrays.equals(ours, theirs as FloatArray)
                        is LongArray -> Arrays.equals(ours, theirs as LongArray)
                        is DoubleArray -> Arrays.equals(ours, theirs as DoubleArray)
                        is Array<*> -> Arrays.equals(ours, theirs as Array<*>)
                        else -> ours == theirs
                    }
                }

    val hashCode by lazy {
        values.entries.sumBy { entry ->
            val (key, value) = entry
            val valueHash = when (value) {
                is BooleanArray -> Arrays.hashCode(value)
                is CharArray -> Arrays.hashCode(value)
                is ByteArray -> Arrays.hashCode(value)
                is ShortArray -> Arrays.hashCode(value)
                is IntArray -> Arrays.hashCode(value)
                is FloatArray -> Arrays.hashCode(value)
                is LongArray -> Arrays.hashCode(value)
                is DoubleArray -> Arrays.hashCode(value)
                is Array<*> -> Arrays.hashCode(value)
                else -> value.hashCode()
            }
            127 * key.hashCode() xor valueHash
        }
    }

    val toString by lazy {
        buildString {
            append('@')
            append(annotationClass.canonicalName)
            values.entries.joinTo(this, separator = ", ", prefix = "(", postfix = ")") { entry ->
                val (key, value) = entry
                val valueString = when (value) {
                    is BooleanArray -> Arrays.toString(value)
                    is CharArray -> Arrays.toString(value)
                    is ByteArray -> Arrays.toString(value)
                    is ShortArray -> Arrays.toString(value)
                    is IntArray -> Arrays.toString(value)
                    is FloatArray -> Arrays.toString(value)
                    is LongArray -> Arrays.toString(value)
                    is DoubleArray -> Arrays.toString(value)
                    is Array<*> -> Arrays.toString(value)
                    else -> value.toString()
                }
                "$key=$valueString"
            }
        }
    }

    val result = Proxy.newProxyInstance(annotationClass.classLoader, arrayOf(annotationClass)) { _, method, args ->
        val name = method.name
        when (name) {
            "annotationType" -> annotationClass
            "toString" -> toString
            "hashCode" -> hashCode
            else -> when {
                name == "equals" && args?.size == 1 -> equals(args.single())
                values.containsKey(name) -> values[name]
                else -> throw KotlinReflectionInternalError("Method is not supported: $method (args: ${args.orEmpty().toList()})")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    return result as T
}
