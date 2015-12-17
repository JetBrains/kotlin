// WITH_RUNTIME
fun foo() {
    val list = 1..4

    <caret>for (x in list) {
        // comment
        var v = x + 1
    }
}