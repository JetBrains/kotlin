// WITH_RUNTIME
// IS_APPLICABLE: true

fun foo() {
    val foo: String? = null
    foo?.let {
        it.to("").to("")<caret>
    }
}