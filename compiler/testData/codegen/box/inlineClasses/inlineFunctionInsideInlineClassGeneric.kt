// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Foo<T: Int>(val x: T) {
    inline fun inc(): Foo<T> = Foo(x + 1) as Foo<T>
}

fun box(): String {
    val a = Foo(0)
    val b = a.inc().inc()

    if (b.x != 2) return "fail"

    return "OK"
}