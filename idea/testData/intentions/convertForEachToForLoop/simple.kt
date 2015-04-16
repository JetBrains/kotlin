// WITH_RUNTIME
fun foo() {
    val x = 1..4

    <caret>x.forEach { it.equals(1) }
}