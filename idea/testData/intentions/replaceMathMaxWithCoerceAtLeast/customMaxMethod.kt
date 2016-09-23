// WITH_RUNTIME
// IS_APPLICABLE: false

fun foo() {
    Math.max(1, 2)<caret>
}

object Math {
    fun max(a: Int, b: Int) = 0
}
