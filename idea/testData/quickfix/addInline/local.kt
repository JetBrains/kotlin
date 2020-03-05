// "Add 'inline' to function 'foo'" "false"
// ACTION: Enable a trailing comma by default in the formatter
// ERROR: Modifier 'crossinline' is allowed only for function parameters of an inline function

fun bar() {
    fun foo(<caret>crossinline body: () -> Unit) {

    }
}
