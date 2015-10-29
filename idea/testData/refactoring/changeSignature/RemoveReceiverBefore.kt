fun X.<caret>foo(s: String, k: Int): Boolean {
    return this.k + s.length - k > 0
}

class X(val k: Int)

fun test() {
    with(X(0)) {
        foo("1", 2)
    }
    X(0).foo("1", 2)
}