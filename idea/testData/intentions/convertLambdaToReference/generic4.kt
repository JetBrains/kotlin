// WITH_RUNTIME
fun test() {
    listOf(listOf(1)).filter <caret>{ it.isNotEmpty() }
}