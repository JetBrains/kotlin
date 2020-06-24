// WITH_RUNTIME
fun test(list: List<Double>) {
    list.fold<caret>(0.0) { acc, i -> acc + i }
}