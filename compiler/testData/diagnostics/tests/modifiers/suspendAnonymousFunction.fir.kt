// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-57991

fun foo() {
    <!ANONYMOUS_SUSPEND_FUNCTION!>suspend<!> fun() {

    }
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration */
