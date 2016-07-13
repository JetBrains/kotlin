// !DIAGNOSTICS: -UNUSED_VARIABLE

fun foo(x: Any) {
    x.<!SYNTAX!><!>
    val foo = 1

    x.<!SYNTAX!><!>
    fun bar() = 2

    x.
    <!ILLEGAL_SELECTOR!>fun String.() = 3<!>

    var a = 24.<!SYNTAX!><!>
    var b = 42.0
}

class A {
    val z = "a".<!SYNTAX!><!>
    val x = 4

    val y = "b".<!SYNTAX!><!>
    fun baz() = 5

    val q = "c".
    <!ILLEGAL_SELECTOR!>fun String.() = 6<!>

    var a = 24.<!SYNTAX!><!>
    var b = 42.0
}
