// "Implement members" "true"
// ERROR: Class 'ExpImpl' is not abstract and does not implement abstract member public abstract actual fun first(): Unit defined in ExpInterface

actual interface ExpInterface {
    actual fun first()
}

actual class ExpImpl<caret> : ExpInterface