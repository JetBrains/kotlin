// WITH_RUNTIME
// INTENTION_TEXT: Replace '==' with 'Arrays.equals'
fun foo() {
    val a = charArrayOf('a', 'b', 'c')
    val b = charArrayOf('a', 'b', 'c')
    if (a <caret>== b) {
    }
}
