// RUN_PIPELINE_TILL: FRONTEND
fun foo(
    f: <!REPEATED_MODIFIER!>suspend<!> suspend () -> Unit
) {}