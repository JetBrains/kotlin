class Test {
    private var x = object {};
    {
        x = <!TYPE_MISMATCH!>object<!> {}
    }
}