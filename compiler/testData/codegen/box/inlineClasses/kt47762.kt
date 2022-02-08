// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val a: Int = 1) {
    companion object {
        val a: Int = 2
    }
}

fun box(): String {
    if (A.a != 2) return "FAIL1"
    val instance = A()
    return if (instance.a != 1) "FAIL2" else "OK"
}
