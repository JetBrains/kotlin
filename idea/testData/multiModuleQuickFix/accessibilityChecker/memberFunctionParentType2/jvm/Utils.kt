// "Create expected class in common module testModule_Common" "true"
// DISABLE-ERRORS

actual class A<T> {
    class B {
        actual fun <caret>a(): T = TODO()
    }
}