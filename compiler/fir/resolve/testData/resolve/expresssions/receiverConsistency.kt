fun foo() {}

class C {
    fun bar() {}
    fun err() {}

    class Nested {
        fun test() {
            err()
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