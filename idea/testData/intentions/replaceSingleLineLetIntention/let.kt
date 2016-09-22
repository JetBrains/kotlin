// WITH_RUNTIME
// IS_APPLICABLE: true

fun foo() {
    val foo: String? = null
    foo?.let {
        it.length<caret>
    }
}