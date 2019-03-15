// WITH_RUNTIME

fun test(list: List<Int>?): List<Int>? {
    return list?.filter { it > 1 }!!.<caret>filter { it > 2 }.filter { it > 3 }
}