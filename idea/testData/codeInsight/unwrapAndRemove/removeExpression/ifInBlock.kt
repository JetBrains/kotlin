// OPTION: 0
fun foo(n : Int): Int {
    <caret>if (n > 0) {
        1
    } else {
        -1
    }

    return 0
}
