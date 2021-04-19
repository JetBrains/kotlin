class My {
    var x: String = ""
        set(vararg value) {
            x = <!ASSIGNMENT_TYPE_MISMATCH!>value<!>
        }
}
