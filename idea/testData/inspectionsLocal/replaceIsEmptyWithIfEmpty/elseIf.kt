// PROBLEM: none
// WITH_RUNTIME
fun test(list: List<Int>, b: Boolean): List<Int> {
    return if (list.isEmpty<caret>()) {
        listOf(1)
    } else if (b) {
        listOf(2)
    } else {
        list
    }
}