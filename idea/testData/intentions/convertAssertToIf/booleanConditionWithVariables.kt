// WITH_RUNTIME
fun foo() {
    val x = true
    val y = false
    <caret>assert(x || y) { "text" }
}