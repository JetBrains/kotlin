// WITH_RUNTIME
fun test(list: List<String>) {
    val b = list.<caret>all { it.isNotEmpty() }
}