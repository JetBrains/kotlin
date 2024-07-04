// ISSUE: KT-27112

// FILE: Foo.kt
private open class Foo {
    fun bar() {}
}

fun <T : Foo> foo(x: T?) = x

// FILE: Main.kt
fun box() = "OK".also {
    foo(null)?.<!UNRESOLVED_REFERENCE!>bar<!>()
}
