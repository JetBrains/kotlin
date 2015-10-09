// IS_APPLICABLE: false
fun Int.xxx(p: Int) = 1

fun foo(x: Int) {
    x.<caret>xxx(1)
}
