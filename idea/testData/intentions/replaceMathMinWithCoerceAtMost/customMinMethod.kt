// WITH_RUNTIME
// IS_APPLICABLE: false

fun foo() {
    Math.min(1, 2)<caret>
}

object Math {
    fun min(a: Int, b: Int) = 0
}
