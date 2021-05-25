package foo.bar

fun test() {
    class A {
        inner class B
    }

    fun <!UNRESOLVED_REFERENCE!>A.B<!>.foo() {}
}
