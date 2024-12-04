inline fun <reified T : Any> foo(init: <caret>T.() -> Unit = {}): T {
    TODO("message")
}
