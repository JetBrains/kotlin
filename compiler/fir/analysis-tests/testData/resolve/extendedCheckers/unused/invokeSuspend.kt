suspend fun foo(action: suspend () -> Unit) {
    val x = action

    x()
}
