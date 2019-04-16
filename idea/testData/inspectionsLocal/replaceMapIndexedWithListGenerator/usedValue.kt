// PROBLEM: none
// WITH_RUNTIME
fun test(list: List<String>) {
    list.<caret>mapIndexed { index, value ->
        println(value)
        index + 42
    }
}