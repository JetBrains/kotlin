// WITH_RUNTIME


fun foo() {
    val foo: String? = null
    foo?.let<caret> {
        text ->
        text.length
    }
}