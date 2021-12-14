// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val x: String)

class B {
    override fun equals(other: Any?) = true
}

fun box(): String {
    val x: Any? = B()
    val y: A = A("")
    if (x != y) return "Fail"
    return "OK"
}
