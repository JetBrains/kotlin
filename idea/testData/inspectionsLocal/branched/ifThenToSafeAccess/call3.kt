// WITH_RUNTIME
// HIGHLIGHT: INFORMATION
fun convert(x: String, y: Int) = ""

fun foo(a: String?, it: Int) {
    <caret>if (a != null) convert(a, it) else null
}
