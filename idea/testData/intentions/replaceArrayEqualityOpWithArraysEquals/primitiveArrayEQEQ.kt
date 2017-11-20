// WITH_RUNTIME
// INTENTION_TEXT: Replace '==' with 'contentEquals'
fun foo() {
    val a = charArrayOf('a', 'b', 'c')
    val b = charArrayOf('a', 'b', 'c')
    if (a <caret>== b) {
    }
}
