// FIR_IDENTICAL
interface IBase<T> {
    fun foo(x: Int)
    val bar: Int
    fun <X> qux(t: T, x: X)
}

class Test<TT>(impl: IBase<TT>) : IBase<TT> by impl
