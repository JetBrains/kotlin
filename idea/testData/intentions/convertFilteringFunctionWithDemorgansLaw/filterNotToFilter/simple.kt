// WITH_RUNTIME
fun test(list: List<Int>) {
    val b = list.filterNot<caret> { it != 1 }
}