// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
    fun f(x: Boolean): Int = 0

    fun f(y: String): Int = 0
}

class B {
    private var a: A? = null

    fun takeInt(i: Int) {}

    fun f() {
        a = A()
        a.<!INAPPLICABLE_CANDIDATE!>f<!>(true)
        takeInt(a.<!INAPPLICABLE_CANDIDATE!>f<!>(""))
        a.<!INAPPLICABLE_CANDIDATE!>f<!>()
    }

    fun g() {
        takeInt(if (3 > 2) {
            a = A()
            a.<!INAPPLICABLE_CANDIDATE!>f<!>(true)
        } else {
            6
        })
    }
}