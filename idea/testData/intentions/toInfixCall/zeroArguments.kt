// IS_APPLICABLE: false
// ERROR: No value passed for parameter p
infix fun Int.xxx(p: Int) = 1

fun foo(x: Int) {
    x.<caret>xxx()
}
