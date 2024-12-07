// RUN_PIPELINE_TILL: FRONTEND
class Test {
    private var x = object {};
    init {
        x = <!TYPE_MISMATCH!>object<!> {}
    }
}