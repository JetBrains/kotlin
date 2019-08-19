// "Create expected class in common module testModule_Common" "true"
// DISABLE-ERRORS

actual sealed class <caret>My actual constructor(actual val x: Double) {

    actual abstract val num: Int

    actual open fun isGood() = false

    actual object First : My(1.0) {
        actual override val num = 0
    }

    actual class Some actual constructor(actual override val num: Int) : My(num.toDouble())

    actual object Best : My(999.0) {
        actual override val num = 42

        actual override fun isGood() = true
    }
}