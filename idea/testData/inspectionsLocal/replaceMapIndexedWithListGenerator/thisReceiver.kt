// WITH_RUNTIME
fun List<String>.test() {
    this.<caret>mapIndexed { index, _ ->
        index + 42
    }
}