// WITH_RUNTIME
// INTENTION_TEXT: Replace '==' with 'Arrays.equals'
fun foo() {
    val a = arrayOf("a", "b", "c")
    val b = arrayOf("a", "b", "c")
    if (a <caret>== b) {
    }
}
