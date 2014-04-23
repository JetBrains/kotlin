// WITH_RUNTIME
fun foo() {
    if <caret>(true) {
        throw AssertionError(null)
    }
}