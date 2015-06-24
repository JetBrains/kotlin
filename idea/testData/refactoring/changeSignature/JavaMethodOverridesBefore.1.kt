open class X: A() {
    fun foo(s: String): Int {
        return super.foo(s)
    }
}

open class Y: B() {
    fun foo(s: String): Int {
        return 0
    }
}

open class Z: X() {
    fun foo(s: String): Int {
        return 0
    }
}

fun test() {
    A().foo("")
    B().foo("")
    X().foo("")
    Y().foo("")
    Z().foo("")
}