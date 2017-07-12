// "Remove useless is check" "false"
fun foo(a: String) {
    when (1) {
        <caret>is Int -> { }
    }
}
