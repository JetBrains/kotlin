// IS_APPLICABLE: true
fun foo() {
    val x = 4
    val y = "$x$<caret>x$x"
}