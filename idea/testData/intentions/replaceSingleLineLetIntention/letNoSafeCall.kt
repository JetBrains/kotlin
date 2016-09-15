// WITH_RUNTIME

fun foo() {
    val foo: String = ""
    foo.let {
        it.length<caret>
    }
}