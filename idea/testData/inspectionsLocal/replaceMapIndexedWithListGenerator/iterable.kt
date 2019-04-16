// PROBLEM: none
// WITH_RUNTIME
fun test(list: Iterable<String>) {
    list.<caret>mapIndexed { index, _ ->
        index + 42
    }
}