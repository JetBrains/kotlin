// "Add missing actual members" "true"
// DISABLE-ERRORS

actual class <caret>My {
    actual fun foo(param: String) = 42

    actual val correct = true
}
