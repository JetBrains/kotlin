interface I {
    val prop: Int
}

open class A {
    fun f1() {
        this as I
        <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>prop<!>
    }

    fun f2() {
        <!UNRESOLVED_REFERENCE!>prop<!>
    }
}

open class B {
    fun f() {
        {
            this as I
            <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>prop<!>
        }
        <!UNRESOLVED_REFERENCE!>prop<!>
    }
}
