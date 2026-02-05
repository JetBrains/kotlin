// TARGET_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +JvmInlineMultiFieldValueClasses
// WITH_SIGNATURES

class Wrapper<T>(val value: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class WithWrapper<T : Any>(val x: Wrapper<T>?)

object Example {
    fun <R : Any> WithWrapper<Int>.genericArgument(x: WithWrapper<R>) {}
    fun WithWrapper<Int>.instantiatedArgument(x: WithWrapper<String>) {}
    fun returnType(x: WithWrapper<String>): WithWrapper<Int> = WithWrapper(Wrapper(1))
    fun nullableReturnType(x: WithWrapper<String>): WithWrapper<Int>? = null
    fun <R : Any> genericReturnType(x: R): WithWrapper<R> = WithWrapper(Wrapper(x))

    val instantiatedProperty: WithWrapper<Int> = WithWrapper(Wrapper(1))
    val nullableProperty: WithWrapper<Int>? = null
}