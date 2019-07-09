// WITH_RUNTIME


fun foo() {
    val foo: String? = null
    foo?.let<caret> {
        it.hashCode().hashCode()
    }
}