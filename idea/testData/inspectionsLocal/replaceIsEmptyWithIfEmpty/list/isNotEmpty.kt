// WITH_RUNTIME
fun test(list: List<Int>): List<Int> {
    return if (list.isNotEmpty<caret>()) {
        list
    } else {
        listOf(1)
    }
}