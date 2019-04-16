// WITH_RUNTIME
fun List<String>.test() {
    <caret>mapIndexed { index, _ ->
        index + 42
    }
}