// WITH_RUNTIME

fun test(list: List<Int>): List<Int> {
    return <caret>list
            .reversed()
            .map { it + 1 }
            .map { it + 1 }
            .dropLast(1)
            .takeLast(2)
}