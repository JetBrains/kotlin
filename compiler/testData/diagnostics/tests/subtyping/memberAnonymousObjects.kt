class Test {
    private var x = object {};
    init {
        x = <!TYPE_MISMATCH!>object<!> {}
    }
}