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
        a.<!NONE_APPLICABLE!>f<!>(true)
        takeInt(a.<!NONE_APPLICABLE!>f<!>(""))
        a.<!NONE_APPLICABLE!>f<!>()
    }

    fun g() {
        takeInt(if (3 > 2) {
            a = A()
            a.<!NONE_APPLICABLE!>f<!>(true)
        } else {
            6
        })
    }
}
