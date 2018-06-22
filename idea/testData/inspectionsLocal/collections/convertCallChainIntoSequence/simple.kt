// WITH_RUNTIME

fun test(): List<Int> {
    return <caret>listOf(1, 2, 3).filter { it > 1 }.map { it * 2 }
}