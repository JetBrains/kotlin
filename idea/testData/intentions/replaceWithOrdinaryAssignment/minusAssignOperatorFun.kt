// IS_APPLICABLE: false
operator fun Int.minusAssign(element: Int) {}

fun test() {
    val x = 1
    x <caret>-= 9
}