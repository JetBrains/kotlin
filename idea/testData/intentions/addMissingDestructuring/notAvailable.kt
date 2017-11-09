// IS_APPLICABLE: false
data class Foo(val a: String, val b: String, val c: String)

fun bar(f: Foo) {
    val (a, b, c<caret>) = f
}
