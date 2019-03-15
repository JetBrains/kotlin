suspend fun foo(
    block: suspend () -> Unit
) {
    block()
}
