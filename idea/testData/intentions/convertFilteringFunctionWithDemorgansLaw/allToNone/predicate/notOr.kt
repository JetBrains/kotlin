// WITH_RUNTIME
fun test(list: List<Int>) {
    val b = list.<caret>all { !(it != 1 || it != 2) }
}