// RUN_PIPELINE_TILL: FRONTEND
fun foo(
    f: suspend suspend () -> Unit
) {}