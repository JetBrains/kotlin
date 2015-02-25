class A(val k: Int) {
    fun X.<caret>foo(s: String, k: Int): Boolean {
        return this.k + s.length() - k + this@A.k/2 > 0
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