// PROBLEM: none
// WITH_RUNTIME
fun test(b: Boolean) {
    val list = mutableListOf<Int>()
    <caret>if (b) {
        list += 1
    } else {
        list += mutableListOf(2)
    }
}