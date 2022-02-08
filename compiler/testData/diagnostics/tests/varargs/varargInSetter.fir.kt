class My {
    var x: String = ""
        set(<!WRONG_MODIFIER_CONTAINING_DECLARATION!>vararg<!> value) {
            x = <!ASSIGNMENT_TYPE_MISMATCH!>value<!>
        }
}
