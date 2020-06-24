// WITH_RUNTIME
fun test(list: List<Long>) {
    list.reduce<caret> { acc, i -> acc + i }
}