open class X: A() {
    fun foo(x: Int): String? {
        return super.foo(1) + 1
    }
}

open class Y: B() {
    fun foo(x: Int): String? {
        return s.length() * 2
    }
}

open class Z: X() {
    fun foo(x: Int): String? {
        return s.length()
    }
}

fun test() {
    A().foo(1)
    B().foo(1)
    X().foo(1)
    Y().foo(1)
    Z().foo(1)
}