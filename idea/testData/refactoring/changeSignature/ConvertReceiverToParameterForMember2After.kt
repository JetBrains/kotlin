class A(val k: Int) {
    fun foo(s: String, x: X, k: Int): Boolean {
        return x.k + s.length - k + this.k/2 > 0
    }

    fun test() {
        foo("1", X(0), 2)
    }
}

class X(val k: Int)

fun test() {
    with(A(3)) {
        foo("1", X(0), 2)
    }
}