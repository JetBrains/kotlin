infix fun String.compareTo(other: String) = 0

fun foo(x: String) {
    x!! <caret>compareTo "1"
}
