// PROBLEM: none

fun foo() {
    val a = arrayOf("a", "b", "c")
    val b: Any? = null
    if (a <caret>== b) {
    }
}
