fun test(list: MutableList<String>) {
    list.<!AMBIGUITY!>removeAll<!> {
        <!UNRESOLVED_REFERENCE!>it<!>.<!AMBIGUITY!>isEmpty<!>()
    }
}
