// WITH_RUNTIME
fun test(list: List<Int>) {
    val b = !list.<caret>none { it == 1 }
}