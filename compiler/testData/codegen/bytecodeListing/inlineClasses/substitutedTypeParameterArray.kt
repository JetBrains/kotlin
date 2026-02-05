// TARGET_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +JvmInlineMultiFieldValueClasses
// WITH_SIGNATURES

OPTIONAL_JVM_INLINE_ANNOTATION
value class WithArray<T : Any>(val x: Array<T>)

OPTIONAL_JVM_INLINE_ANNOTATION
value class WithNestedArray<T : Any>(val x: Array<Array<T>>)

object Example {
    fun <R : Any> WithArray<Int>.genericArgument(x: WithArray<R>) {}
    fun WithArray<Int>.instantiatedArgument(x: WithArray<String>) {}
    fun returnType(x: WithArray<String>): WithArray<Int> = WithArray(arrayOf(1))
    fun nullableReturnType(x: WithArray<String>): WithArray<Int>? = null
    inline fun <reified R : Any> genericReturnType(x: R): WithArray<R> = WithArray(arrayOf(x))

    val instantiatedProperty: WithArray<Int> = WithArray(arrayOf(1))
    val nullableProperty: WithArray<Int>? = null
}

object NestedExample {
    fun <R : Any> WithNestedArray<Int>.genericArgument(x: WithNestedArray<R>) {}
    fun WithNestedArray<Int>.instantiatedArgument(x: WithNestedArray<String>) {}
    fun returnType(x: WithNestedArray<String>): WithNestedArray<Int> = WithNestedArray(arrayOf(arrayOf(1)))
    fun nullableReturnType(x: WithNestedArray<String>): WithNestedArray<Int>? = null
    inline fun <reified R : Any> genericReturnType(x: R): WithNestedArray<R> = WithNestedArray(arrayOf(arrayOf(x)))

    val instantiatedProperty: WithNestedArray<Int> = WithNestedArray(arrayOf(arrayOf(1)))
    val nullableProperty: WithNestedArray<Int>? = null
}