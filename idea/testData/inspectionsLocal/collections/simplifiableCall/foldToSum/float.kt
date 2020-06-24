// WITH_RUNTIME
fun test(list: List<Float>) {
    list.fold<caret>(0.0F) { acc, i -> acc + i }
}