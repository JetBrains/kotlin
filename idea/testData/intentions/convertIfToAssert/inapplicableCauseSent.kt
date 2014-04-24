// IS_APPLICABLE: false
// WITH_RUNTIME
// MIN_JAVA_VERSION: 1.7
fun foo() {
    if <caret>(1 == 0) {
        throw AssertionError("text", Exception())
    }
}
