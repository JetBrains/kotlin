class A

class B: A() {
    fun act() {
        <!SUPER_IS_NOT_AN_EXPRESSION!>super<!>()

        <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>invoke<!>()<!>

        <!SUPER_IS_NOT_AN_EXPRESSION!>super<!> {
            <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>println<!>(<!ILLEGAL_CONST_EXPRESSION!>'weird'<!>)<!>
        }
    }
}
