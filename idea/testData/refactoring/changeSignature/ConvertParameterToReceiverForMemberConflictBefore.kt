class A(val k: Int) {
    fun <caret>foo(x: X, s: String, k: Int): Boolean {
        return x.k + s.length() - k + this.k/2 > 0
    }

    fun test() {
        foo(X(0), "1", 2)
    }
}

class X(val k: Int)

fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

fun test() {
    A(3).foo(X(0), "1", 2)
    with(A(3)) {
        foo(X(0), "1", 2)
    }
}