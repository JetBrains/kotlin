// WITH_RUNTIME

fun foo() {
    val foo: String? = null
    foo?.let {
        it.to("")<caret>
    }
}