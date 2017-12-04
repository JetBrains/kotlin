// WITH_RUNTIME

fun foo(map: MutableMap<Int, String>) {
    map.put<caret>(42, "foo")
}