// WITH_RUNTIME

fun foo() {
    val foo: String? = null
    foo?.let {
        text ->
        text.length<caret>
    }
}