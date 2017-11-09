fun <caret>foo(p1: String, p2: () -> Boolean) = bar(p1, null, p2)

fun bar(p1: String, p2: String?, p3: () -> Boolean)

fun foo(i: I) {
    foo("foo") {
        println("bar")
        println("baz")
    }
}
