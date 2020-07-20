// WITH_RUNTIME

fun test(list: List<Int>) {
    list.<caret>filter { it > 1 }.runningReduce { acc, i -> acc + i }
}