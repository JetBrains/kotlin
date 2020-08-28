// COMPILER_ARGUMENTS: -XXLanguage:+SuspendConversion
fun foo(a: suspend () -> Unit) {}

fun action() {}

fun usage() {
    foo { action() <caret> }
}