// WITH_RUNTIME
fun foo() {
    val x = true
    if <caret>(x && false) {
        throw AssertionError("text")
    }
}