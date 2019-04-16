// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>mapIndexed { index, value ->
        if (index == 0) return@mapIndexed 0
        index + 42
    }
}