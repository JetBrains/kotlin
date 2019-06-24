// WITH_RUNTIME


fun foo() {
    val foo: String = ""
    foo.let<caret> {
        it.length
    }
}