package bar

class S<T> {
    fun foo() {
        <!OTHER_ERROR!>T<!>
        <!OTHER_ERROR!>T<!>.<!UNRESOLVED_REFERENCE!>create<!>()
    }
}