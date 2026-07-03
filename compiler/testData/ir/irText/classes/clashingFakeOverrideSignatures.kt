// IGNORE_BACKEND: JKLIB
// DUMP_IR_DIFFERENCE: JVM
//   Probably, fake overrides for non-JVM targets are built incorrectly, like `FUN FAKE_OVERRIDE name:foo` in class Derived with `VALUE_PARAMETER name:x` overrides both
//     - public final fun foo (x: T of <root>.Base): kotlin.Unit declared in <root>.Base
//     - public final fun foo (y: kotlin.String): kotlin.Unit declared in <root>.Base
// KOTLIN_REFLECT_DUMP_MISMATCH

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
