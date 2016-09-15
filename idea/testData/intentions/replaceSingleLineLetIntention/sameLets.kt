// WITH_RUNTIME

fun foo() {
    val foo: String? = null
    foo?.toString()?.let {
        it.to("")<caret>
    }?.let {
        it.to("")
    }
}