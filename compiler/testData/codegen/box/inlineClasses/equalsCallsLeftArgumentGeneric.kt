// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: String>(val x: T)

class B {
    override fun equals(other: Any?) = true
}

fun box(): String {
    val x: Any? = B()
    val y: A<String> = A("")
    if (x != y) return "Fail"
    return "OK"
}
