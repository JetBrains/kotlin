// PROBLEM: none
// WITH_RUNTIME
fun test() {
    listOf("A").forEach {
        setOf(1).map { <caret>_ -> it.length }
    }
}