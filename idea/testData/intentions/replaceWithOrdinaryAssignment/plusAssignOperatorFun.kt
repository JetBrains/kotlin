// IS_APPLICABLE: false
operator fun Int.plusAssign(element: Int)  {}

fun test() {
    val x = 1
    x <caret>+= 9
}
