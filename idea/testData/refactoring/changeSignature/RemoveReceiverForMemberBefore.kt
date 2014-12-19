class A(val k: Int) {
    fun X.<caret>foo(s: String, n: Int): Boolean {
        return s.length() * this.k - n.inc() + this@A.k > 0
    }

    fun test() {
        X(0).foo("1", 2)
    }
}

class X(val k: Int)

fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

fun test() {
    with(A(3)) {
        X(0).foo("1", 2)
    }
}