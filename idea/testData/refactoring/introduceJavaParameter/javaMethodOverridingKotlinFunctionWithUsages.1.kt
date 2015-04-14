fun test() {
    J().foo(1, 2, 3)
    K().foo(1, 2, 3)
}

abstract class K0 {
    abstract fun foo(a: Int, b: Int, c: Int): Int
}

open class K: K0() {
    override fun foo(a: Int, b: Int, c: Int) = a + b + c
}