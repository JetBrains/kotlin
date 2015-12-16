// WITH_RUNTIME
fun foo() {
    val list = 1..4

    <caret>for (x in list) {
        x
    }
}