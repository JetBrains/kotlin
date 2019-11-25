// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

interface IFoo<T> {
    fun foo(x: T): String
}

inline class Z(val x: Int) : IFoo<Z> {
    override fun foo(x: Z) = "OK"
}

fun box(): String =
    Z(1).foo(Z(2))