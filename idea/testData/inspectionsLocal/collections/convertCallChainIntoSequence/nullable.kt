// WITH_RUNTIME

fun test(list: List<Int>?): List<Int>? {
    return list
            ?.<caret>filter { it > 1 }
            ?.map { it * 2 }
}