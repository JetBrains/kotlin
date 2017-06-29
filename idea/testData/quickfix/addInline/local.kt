// "Add 'inline' to function 'foo'" "false"
// ERROR: Modifier 'crossinline' is allowed only for function parameters of an inline function

fun bar() {
    fun foo(<caret>crossinline body: () -> Unit) {

    }
}
