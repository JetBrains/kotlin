fun main() {
    val a = 10

    <caret>if (a > 0)
        a
    else if (a < -5)
        a + 1
    else
        a + 2
}