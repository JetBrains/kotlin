class A(val k: Int) {
    fun String.foo(x: X, k: Int): Boolean {
        return x.k + length - k + this@A.k/2 > 0
    }

    fun test() {
        "1".foo(X(0), 2)
    }
}

class X(val k: Int)

fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

fun test() {
    with(A(3)) {
        "1".foo(X(0), 2)
    }
}