// WITH_RUNTIME
fun main() {
    val x = 1..4

    <caret>x.reverse().forEach { it }
}