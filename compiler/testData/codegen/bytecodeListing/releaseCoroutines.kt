// !API_VERSION: 1.3

suspend fun named() {}

suspend fun withStateMachine() {
    named()
    named()
}

val l: suspend() -> Unit = {}
