// WITH_RUNTIME
fun test(list: List<Long>) {
    list.fold<caret>(0L) { acc, i -> acc + i }
}