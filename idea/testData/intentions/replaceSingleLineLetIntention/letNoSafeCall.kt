// WITH_RUNTIME
// IS_APPLICABLE: true

fun foo() {
    val foo: String = ""
    foo.let {
        it.length<caret>
    }
}