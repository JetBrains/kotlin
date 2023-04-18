// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57754

interface IBase<T> {
    fun foo(x: Int)
    val bar: Int
    fun <X> qux(t: T, x: X)
}

class Test<TT>(impl: IBase<TT>) : IBase<TT> by impl
