class Test {
    private var x = object {};
    init {
        x = <!ASSIGNMENT_TYPE_MISMATCH!>object<!> {}
    }
}