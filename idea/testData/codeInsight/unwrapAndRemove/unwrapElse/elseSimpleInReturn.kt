// OPTION: 2
fun foo(n: Int): Int {
    return if (n > 0) {
        n + 10
    } <caret>else {
        n - 10
    }
}
