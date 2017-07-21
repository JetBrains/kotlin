// "Remove useless is check" "true"
fun foo(a: String) {
    when (1) {
        1 -> 0
        <caret>!is Int -> 42
    }
}
