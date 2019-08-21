// "Create expected class in common module testModule_Common" "true"
// DISABLE-ERRORS

class A {
    actual class M
    actual fun <caret>a(): M = TODO()
}