// PROBLEM: none
fun test(b: Boolean): Unit = if (b) {
    fun a() = 1
    <caret>Unit
} else {
}