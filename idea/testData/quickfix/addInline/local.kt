// "Add 'inline' to function 'foo'" "false"
// ACTION: Convert to expression body
// ERROR: Modifier 'crossinline' is allowed only for function parameters of an inline function

fun bar() {
    fun foo(<caret>crossinline body: () -> Unit) {

    }
}
