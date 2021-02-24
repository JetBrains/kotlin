// !LANGUAGE: +InlineClasses

interface IFoo<T> {
    fun foo(a: T)
}

inline class Z(val x: Int)

inline class CFoo(val x: Long) : IFoo<Z> {
    override fun foo(a: Z) {}
}