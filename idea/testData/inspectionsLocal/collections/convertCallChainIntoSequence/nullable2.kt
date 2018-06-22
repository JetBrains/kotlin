// WITH_RUNTIME

fun test(list: List<Int>?): List<Int>? {
    return <caret>list?.filter { it > 1 }!!.filter { it > 2 }.filter { it > 3 }
}