// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val x: Int) {
    fun f(): Int = super.hashCode()
}

fun box(): String {
    val a = A(1).f()
    return "OK"
}
