// DIAGNOSTICS: -UNUSED_PARAMETER

class A {
    fun f(x: Boolean): Int = 0

    fun f(y: String): Int = 0
}

class B {
    private var a: A? = null

    fun takeInt(i: Int) {}

    fun f() {
        a = A()
        a<!UNSAFE_CALL!>.<!>f(true)
        takeInt(a<!UNSAFE_CALL!>.<!>f(""))
        a.<!NONE_APPLICABLE!>f<!>()
    }

    fun g() {
        takeInt(if (3 > 2) {
            a = A()
            a<!UNSAFE_CALL!>.<!>f(true)
        } else {
            6
        })
    }
}
