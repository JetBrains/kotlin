// PROBLEM: Call chain on collection could be converted into 'Sequence' to improve performance
// HIGHLIGHT: GENERIC_ERROR_OR_WARNING
// WITH_RUNTIME

fun test(): List<Int> {
    return listOf(1, 2, 3).<caret>filter { it > 1 }.map { it * 2 }.map { it * 3 }.map { it * 4 }.map { it * 5 }
}