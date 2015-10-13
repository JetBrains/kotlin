// IS_APPLICABLE: false
fun String.xxx(p: String): Int = 0

fun foo(x: String?) {
    x?.<caret>xxx("1")
}
