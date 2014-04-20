// IS_APPLICABLE: false
// WITH_RUNTIME
fun foo() {
    if <caret>(1 == 0) {
        val y = 1
        throw AssertionError("text")
    }
}