// RUN_PIPELINE_TILL: BACKEND
fun test(func: () -> String?) {
    val x = func() ?: ""
}
