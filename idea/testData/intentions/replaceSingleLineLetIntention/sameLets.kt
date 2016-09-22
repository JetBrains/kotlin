// WITH_RUNTIME
// IS_APPLICABLE: true

fun foo() {
    val foo: String? = null
    foo?.toString()?.let {
        it.to("")<caret>
    }?.let {
        it.to("")
    }
}