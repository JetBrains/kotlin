// WITH_STDLIB

interface A<caret_base>A<T, X> : BB<T, X>, CC<T, X>

interface BB<K, I> : DD<K, I>
interface CC<E, U> : DD<E, U>

interface D<caret_super>D<M, N> {
    context(_: Pair<M, N>)
    fun Generic<M, N>.foo(): Pair<M, N>

    context(_: Pair<M, N>)
    val Generic<M, N>.bar: Pair<M, N>
}

class Generic<A, B>