// PROBLEM: none
// WITH_RUNTIME

fun test(list: List<Int>): List<Int> {
    return list
            .reversed()
            .map { it + 1 }
            .map { it + 1 }
            <caret>.dropLast(1)
            .takeLast(2)
}
