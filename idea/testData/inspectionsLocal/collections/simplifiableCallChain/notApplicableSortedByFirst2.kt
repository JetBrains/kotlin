// PROBLEM: none
// WITH_RUNTIME
data class Foo(val x: Int?)

fun main() {
    listOf(Foo(1), Foo(null), Foo(2)).<caret>sortedBy { it.x }.first()
}