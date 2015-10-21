open class X: A() {
    override fun foo(s: String): Int {
        return super.foo(s) + 1
    }
}

open class Y: B() {
    override fun foo(s: String): Int {
        return s.length * 2
    }
}

open class Z: X() {
    override fun foo(s: String): Int {
        return s.length
    }
}

fun test() {
    A().foo("")
    B().foo("")
    X().foo("")
    Y().foo("")
    Z().foo("")
}