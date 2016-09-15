// WITH_RUNTIME
// IS_APPLICABLE: false

fun foo() {
    val foo: String? = null
    foo?.let {
        text ->
        text.to(text)<caret>
    }
}