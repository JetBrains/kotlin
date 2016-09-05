// WITH_RUNTIME

fun foo() {
    val foo: String? = null
    foo?.let {
        it.length<caret>
    }
}