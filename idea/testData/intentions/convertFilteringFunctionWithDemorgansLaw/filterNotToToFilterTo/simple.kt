// WITH_RUNTIME
fun test(list: List<Int>) {
    val filteredList = mutableListOf<Int>()
    val b = list.<caret>filterNotTo(filteredList) { it != 1 }
}