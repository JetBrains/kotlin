// WITH_RUNTIME
fun test(list: List<Float>) {
    list.reduce<caret> { acc, i -> acc + i }
}