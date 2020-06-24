// WITH_RUNTIME
fun test(list: List<Double>) {
    list.reduce<caret> { acc, i -> acc + i }
}