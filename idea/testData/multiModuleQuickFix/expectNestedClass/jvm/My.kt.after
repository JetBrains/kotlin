// "Create expected class in common module testModule_Common" "true"
// DISABLE-ERRORS

actual class My {
    actual inner class <caret>Nested(actual val s: String) {
        actual fun hello() = s

        actual var ss = s

        actual class OtherNested(actual var d: Double) {
            actual val dd = d
        }
    }
}