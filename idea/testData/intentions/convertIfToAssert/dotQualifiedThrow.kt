// WITH_RUNTIME
fun foo() {
    if <caret>(true) {
        throw java.lang.AssertionError("text")
    }
}