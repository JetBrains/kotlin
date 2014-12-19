fun <caret>foo(s: String, k: Int): Boolean {
    return s.length() - k > 0
}

class X(val k: Int)

fun test() {
    foo("1", 2)
}