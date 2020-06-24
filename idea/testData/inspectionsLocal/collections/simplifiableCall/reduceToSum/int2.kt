// WITH_RUNTIME
fun test(list: List<Int>) {
    list.reduce<caret> { acc, i -> i + acc }
}