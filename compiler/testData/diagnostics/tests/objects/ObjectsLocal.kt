package localObjects

object A {
    val x : Int = 0
}

open class Foo {
    fun foo() : Int = 1
}

fun test() {
    A.x
    val b = object : Foo() {
    }
    b.foo()

    object B {
        fun foo() {}
    }
    B.foo()
}

val bb = <!UNRESOLVED_REFERENCE!>B<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!>()