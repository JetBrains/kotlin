package one

class A {
    context(p1: A, _: Short)
    fun foo(a: Int, b: String): Boolean = false

    context(p1: A, _: Short)
    fun String.foo2(a: Int, b: String): Boolean = false

    context(_: String, i: Int)
    val bar: Boolean get() = true

    context(_: String, i: Int)
    val Long.bar: Boolean get() = true
}

context(p1: A, _: Short)
fun foo(a: Int, b: String): Boolean = false

context(p1: A, _: Short)
fun String.foo2(a: Int, b: String): Boolean = false

context(_: String, i: Int)
val bar: Boolean get() = true

context(_: String, i: Int)
val Long.bar: Boolean get() = true

// LANGUAGE: +ContextParameters