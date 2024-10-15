// RUN_PIPELINE_TILL: SOURCE
// FIR_DUMP

fun foo() {
    <!ANONYMOUS_SUSPEND_FUNCTION!>suspend<!> fun() {
        bar()
    }
}

suspend fun bar() {

}
