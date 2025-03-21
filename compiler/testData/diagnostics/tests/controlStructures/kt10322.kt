// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
fun test1() {
    run {
        if (true) {
            <!INVALID_IF_AS_EXPRESSION!>if<!> (true) {}
        }
        else {
            1
        }
    }
}
