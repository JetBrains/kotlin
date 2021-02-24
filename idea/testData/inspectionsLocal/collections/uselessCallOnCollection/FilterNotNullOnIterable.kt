// WITH_RUNTIME

fun test(iterable: Iterable<Int>): List<Int> {
    return iterable.<caret>filterNotNull()
}