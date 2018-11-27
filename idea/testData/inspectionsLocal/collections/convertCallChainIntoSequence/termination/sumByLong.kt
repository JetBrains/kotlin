// WITH_RUNTIME

fun test(list: List<Int>) {
    val sumByLong: Long = list.<caret>filter { it > 1 }.sumByLong { it.toLong() }
}
