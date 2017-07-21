// "Remove useless is check" "true"
fun foo(a: String) {
    when {
        1 <caret>is Int -> 0
    }
}
