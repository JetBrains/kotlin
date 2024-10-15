// RUN_PIPELINE_TILL: SOURCE
class Test {
    private var x = object {};
    init {
        x = <!ASSIGNMENT_TYPE_MISMATCH!>object<!> {}
    }
}