// WITH_RUNTIME
// IS_APPLICABLE: false

fun foo() {
    val foo: String? = null
    foo?.let {
        it.to("").to(it).to("")<caret>
    }
}