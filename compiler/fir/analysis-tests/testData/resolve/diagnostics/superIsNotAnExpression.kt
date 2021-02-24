class A

class B: A() {
    fun act() {
        <!SUPER_IS_NOT_AN_EXPRESSION!>super<!>()

        <!UNRESOLVED_REFERENCE!>invoke<!>()

        <!SUPER_IS_NOT_AN_EXPRESSION!>super<!> {
            <!UNRESOLVED_REFERENCE!>println<!>(<!ILLEGAL_CONST_EXPRESSION!>'weird'<!>)
        }
    }
}
