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

val bb = <!UNRESOLVED_REFERENCE!>B<!>.<!UNRESOLVED_REFERENCE!>foo<!>()
