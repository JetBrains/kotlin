// WITH_RUNTIME
fun test(list: List<Int>) {
    list.fold<caret>(0) { acc, i -> acc + i }
}