fun runSuspend(block: suspend () -> Unit) {}

fun println() {}

fun usage() {
    runSuspend(<caret>{ println() })
}