// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

// FILE: lib.kt
OPTIONAL_JVM_INLINE_ANNOTATION
value class Foo(val x: Int) {
    inline fun inc(): Foo = Foo(x + 1)
}

// FILE: main.kt
fun box(): String {
    val a = Foo(0)
    val b = a.inc().inc()

    if (b.x != 2) return "fail"

    return "OK"
}