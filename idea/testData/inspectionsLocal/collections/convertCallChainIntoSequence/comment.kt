// WITH_RUNTIME

fun test(list: List<Int>): Int {
    return list // comment1
            .<caret>filter { it > 1 } // comment2
            .map { it * 2 } // comment3
            .sum() // comment4
}