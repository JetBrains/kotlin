// WITH_RUNTIME
fun foo(i: Int) {}

fun test(s: String) {
    <caret>if (s.isNotBlank()) {
        foo(1)
    } else {
        foo(2)
    }
}