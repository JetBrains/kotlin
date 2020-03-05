fun foo() {}

class C {
    fun bar() {}
    fun err() {}

    class Nested {
        fun test() {
            <!UNRESOLVED_REFERENCE!>err<!>()
        }
    }
}

fun test() {
    val c = C()
    foo()
    c.bar()

    val err = C()
    err.<!UNRESOLVED_REFERENCE!>foo<!>()
}