// WITH_STDLIB

class A<caret_base>A<T, S> : BB<List<T>, Map<S, Int>, S>()

open class B<caret_super>B<Q, R, T>  {
    context(p: kotlin.Triple<Q, R, T>)
    fun Generic<Q, R, T>.foo(): kotlin.Triple<Q, R, T> = TODO()

    context(p: kotlin.Triple<Q, R, T>)
    val Generic<Q, R, T>.bar: kotlin.Triple<Q, R, T> get() = TODO()
}


class Generic<A, B, C>