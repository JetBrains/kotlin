open external class A {
    open fun f(x: Int = noImpl)
}

class B : A() {
    <!OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS!>override fun f(x: Int)<!> {}
}

external class C : A {
    override fun f(x: Int)
}


external interface I {
    fun f(x: Int = noImpl)
}

interface J {
    fun f(x: Int = 23)
}

open external class D {
    open fun f(x: Int)
}

class E : D() {
    override fun f(x: Int) { }
}

class F : D(), I {
    <!OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS!>override fun f(x: Int)<!> {}
}

external class G : D, I {
    override fun f(x: Int)
}

open class X {
    fun f(<!UNUSED_PARAMETER!>x<!>: Int) {}
}

open external class XE {
    fun f(x: Int)
}

class <!OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE!>Y<!> : X(), I

external class YE: XE, I

class Z : X(), J