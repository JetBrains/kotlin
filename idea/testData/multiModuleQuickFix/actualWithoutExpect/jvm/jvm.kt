// "Remove 'actual' modifier" "true"
// ERROR: Actual function 'first' has no corresponding expected declaration

actual interface ExpInterface {
    actual fun first()
}

actual class ExpImpl : ExpInterface {
    actual override fun <caret>first() { }
}