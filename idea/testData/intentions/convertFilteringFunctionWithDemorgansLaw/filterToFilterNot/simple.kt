// WITH_RUNTIME
fun test(list: List<Int>) {
    val b = list.filter<caret> { it != 1 }
}