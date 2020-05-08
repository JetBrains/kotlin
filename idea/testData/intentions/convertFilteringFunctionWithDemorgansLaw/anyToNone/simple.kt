// WITH_RUNTIME
fun test(list: List<Int>) {
    val b = !list.<caret>any { it == 1 }
}