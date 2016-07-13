// !DIAGNOSTICS: -UNUSED_VARIABLE
fun foo(x: Any?) {
    x ?:<!SYNTAX!><!>
    val foo = 1

    x ?:<!SYNTAX!><!>
    fun bar() = 2

    val res: String.() -> Int = null ?:
    fun String.() = 3
}

class A {
    val z = null ?:<!SYNTAX!><!>
    val x = 4

    val y = null ?:<!SYNTAX!><!>
    fun baz() = 5

    val q = null ?:
    fun String.() = 6
}
