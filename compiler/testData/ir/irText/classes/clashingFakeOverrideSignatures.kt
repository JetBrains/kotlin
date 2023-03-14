// SKIP_KLIB_TEST
// See KT-44312
// IGNORE_BACKEND_K2: ANY
//  ^ TODO decide if we want to fix KT-42020 for FIR as well
// IGNORE_BACKEND_K1: JS_IR
// IGNORE_BACKEND_K1: JS_IR_ES6

open class Base<T> {
    fun foo(x: T) {}
    fun foo(y: String) {}

    val T.bar get() = 1
    val String.bar get() = 2
}

open class Derived : Base<String>()

class Derived2 : Derived()

fun test(b: Base<String>, d: Derived, d2: Derived2) {
    b.foo(x = "")
    b.foo(y = "")
    d.foo(x = "")
    d.foo(y = "")
    d2.foo(x = "")
    d2.foo(y = "")
}


open class BaseXY<X, Y> {
    fun foo(x: X, y: String) {}
    fun foo(x: String, y: Y) {}
}

class DerivedXY : BaseXY<String, String>()


fun outerFun() {
    open class LocalBase<T> {
        fun foo(x: T) {}
        fun foo(y: String) {}

        val T.bar get() = 1
        val String.bar get() = 2
    }

    open class LocalDerived : LocalBase<String>()

    class LocalDerived2 : LocalDerived()

    fun test(b: LocalBase<String>, d: LocalDerived, d2: LocalDerived2) {
        b.foo(x = "")
        b.foo(y = "")
        d.foo(x = "")
        d.foo(y = "")
        d2.foo(x = "")
        d2.foo(y = "")
    }
}


open class Outer<T> {
    open inner class Inner {
        fun foo(x: T) {}
        fun foo(y: String) {}
    }
}

class OuterDerived : Outer<String>() {
    inner class InnerDerived : Inner()
}
