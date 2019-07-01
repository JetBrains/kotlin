// WITH_RUNTIME
fun test() {
    setOf(setOf(1)).<caret>flatMap { it }
}