// WITH_RUNTIME
fun foo(i: Int) {}

fun test(s: String) {
    <caret>if (s.isBlank()) {
        foo(1)
    } else {
        foo(2)
    }
}