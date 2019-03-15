// WITH_RUNTIME
fun test() {
    listOf(listOf(1), listOf(2, 3)).<caret>filter { it.size > 1 }.flatten()
}