// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP

fun foo() {
    <!ANONYMOUS_SUSPEND_FUNCTION!>suspend<!> fun() {
        bar()
    }
}

suspend fun bar() {

}
