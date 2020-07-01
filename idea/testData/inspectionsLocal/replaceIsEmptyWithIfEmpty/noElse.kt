// PROBLEM: none
// WITH_RUNTIME
fun test(list: List<Int>) {
    if (list.isEmpty<caret>()) {
        listOf(1)
    }
}