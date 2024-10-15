// RUN_PIPELINE_TILL: SOURCE
class Test {
    private var x = object {};
    init {
        x = <!TYPE_MISMATCH!>object<!> {}
    }
}