// WITH_RUNTIME

fun test(): List<Int> {
    return setOf(1, 2, 3).<caret>filter { it > 1 }.map { it * 2 }
}
