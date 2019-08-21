// "Create expected class in common module testModule_Common" "true"
// DISABLE-ERRORS

interface Some

actual class A {
    actual fun <caret>a(): Some = TODO()
}