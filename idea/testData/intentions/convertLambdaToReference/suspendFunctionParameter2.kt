fun foo(a: suspend () -> Unit) {}

suspend fun action() {}

fun usage() {
    foo { action() <caret> }
}