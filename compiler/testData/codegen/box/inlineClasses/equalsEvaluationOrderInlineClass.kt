// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Inner(val x: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val x: Inner)

var i = 0

fun set1(): A {
    i = 1
    return A(Inner(0))
}

fun test1(n: Int): A {
    if (i != 1)
        throw IllegalStateException("Fail $n")
    i = 0
    return A(Inner(0))
}

fun set1Boxed(): Any? = set1()
fun test1Boxed(n: Int): Any? = test1(n)

fun box(): String {
    try {
        set1() == test1(1)
        set1Boxed() == test1(2)
        set1() == test1Boxed(3)
        set1Boxed() == test1Boxed(4)
    } catch (e: IllegalStateException) {
        return e.message ?: "Fail no message"
    }
    return "OK"
}
