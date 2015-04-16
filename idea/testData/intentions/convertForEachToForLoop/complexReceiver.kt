// WITH_RUNTIME
fun main() {
    val x = 1..4

    x.reverse().forEach<caret> { it }
}