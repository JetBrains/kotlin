class A(val k: Int) {
    fun X.foo(s: String, k: Int): Boolean {
        return this.k + s.length - k + this@A.k/2 > 0
    }

    fun test() {
        X(0).foo("1", 2)
    }
}

class X(val k: Int)

fun test() {
    with(A(3)) {
        X(0).foo("1", 2)
    }
}