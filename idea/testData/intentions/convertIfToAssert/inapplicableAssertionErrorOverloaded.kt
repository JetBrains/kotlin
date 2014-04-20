// IS_APPLICABLE: false
// WITH_RUNTIME
fun foo() {
    if <caret>(1 == 0) {
        throw AssertionError("text")
    }
}

class AssertionError(x: String): Exception(x) {}