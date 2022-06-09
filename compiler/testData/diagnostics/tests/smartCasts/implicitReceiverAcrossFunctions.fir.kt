interface I {
    val prop: Int
}

open class A {
    fun f1() {
        this as I
        prop
    }

    fun f2() {
        <!UNRESOLVED_REFERENCE!>prop<!>
    }
}

open class B {
    fun f() {
        {
            this as I
            prop
        }
        <!UNRESOLVED_REFERENCE!>prop<!>
    }
}
