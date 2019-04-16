// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>mapIndexed({ index, _ -> index + 42 })
}