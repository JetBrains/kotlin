open external class A {
    open fun f(x: Int = definedExternally)
}

class B : A() {
    <!OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS!>override fun f(x: Int)<!> {}
}

class BB : A()

external class C : A {
    override fun f(x: Int)
}


external interface I {
    fun f(x: Int = definedExternally)
}

interface J {
    fun f(x: Int = 23)
}

interface II {
    fun f(x: Int)
}

interface IIJ : II, J

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
    fun f(x: Int) {}
}

open external class XE {
    fun f(x: Int)
}

class <!OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE!>Y<!> : X(), I

class <!OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE!>YY<!> : A(), II

external class YE: XE, I

class Z : X(), J
