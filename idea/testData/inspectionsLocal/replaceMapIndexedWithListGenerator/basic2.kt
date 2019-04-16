// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>mapIndexed { index, value ->
        index + 42
    }
}