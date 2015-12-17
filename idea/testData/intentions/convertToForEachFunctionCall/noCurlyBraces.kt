// WITH_RUNTIME
fun foo() {
    val list = 1..4
    val i = 0

    <caret>for (i in list)
        i
    i
}