// WITH_RUNTIME

fun foo(map: MutableMap<Int, String>) {
    map.<caret>put(42, "foo")
}