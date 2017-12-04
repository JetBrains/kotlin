// WITH_RUNTIME

fun foo() {
    val map = mutableMapOf(42 to "foo")
    map.<caret>put(60, "bar")
}