interface I1 {
    fun foo(x: Int = 1)
}

interface I2 {
    fun bar(x: String = "", y: Int)
}

class A : I1, I2 {
    override fun foo(x: Int) {}
    override fun bar(x: String, y: Int) {}
}

fun foo(a: A) {
    a.foo()
    a.foo(1)

    a.<!INAPPLICABLE_CANDIDATE!>bar<!>()
    a.<!INAPPLICABLE_CANDIDATE!>bar<!>("")
    a.bar(y = 1)
    a.bar("", 2)
}
