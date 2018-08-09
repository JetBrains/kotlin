// WITH_RUNTIME
fun test() {
    listOf(listOf(1)).<caret>flatMap { i -> i }
}