fun <caret>foo(x: X, s: String, k: Int): Boolean {
    return x.k + s.length() - k > 0
}

class X(val k: Int)

fun test() {
    foo(X(0), "1", 2)
}