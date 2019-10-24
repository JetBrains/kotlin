// PROBLEM: none
// WITH_RUNTIME
fun main() {
    listOf(1, null, 2).<caret>sortedBy { it }.first()
}