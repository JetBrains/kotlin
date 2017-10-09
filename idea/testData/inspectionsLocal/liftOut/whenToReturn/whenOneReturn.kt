// HIGHLIGHT: INFO

fun foo(arg: Int): Int {
    <caret>when (arg) {
        0 -> return 0
        else -> throw Exception()
    }
}