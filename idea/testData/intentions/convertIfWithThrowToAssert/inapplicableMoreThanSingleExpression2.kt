// IS_APPLICABLE: false
// WITH_RUNTIME
fun foo() {
    if <caret>(1 == 0) {
        throw AssertionError("text")
        val y = 1
    }
}