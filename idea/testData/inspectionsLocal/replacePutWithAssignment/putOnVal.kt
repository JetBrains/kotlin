// WITH_RUNTIME

fun foo() {
    val map = mutableMapOf(42 to "foo")
    map.put<caret>(60, "bar")
}