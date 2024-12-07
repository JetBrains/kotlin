// RUN_PIPELINE_TILL: BACKEND
suspend fun foo(action: suspend () -> Unit) {
    val x = action

    x()
}
