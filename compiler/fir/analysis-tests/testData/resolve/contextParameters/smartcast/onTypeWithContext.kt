// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

fun test(a: (context(String) () -> Unit)?) {
    if (a != null) {
        a("")
    }
}