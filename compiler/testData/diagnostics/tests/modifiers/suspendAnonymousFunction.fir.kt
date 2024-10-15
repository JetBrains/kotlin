// RUN_PIPELINE_TILL: SOURCE
// ISSUE: KT-57991

fun foo() {
    <!ANONYMOUS_SUSPEND_FUNCTION!>suspend<!> fun() {

    }
}
