// WITH_RUNTIME

fun foo() {
    var map = mutableMapOf(42 to "foo")
    map.put<caret>(60, "bar")
}