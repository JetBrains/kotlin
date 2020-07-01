// PROBLEM: none
// WITH_RUNTIME
fun test(list: List<Int>): List<Int> {
    return if (!list.isEmpty<caret>()) {
        list
    } else {
        listOf(1)
    }
}