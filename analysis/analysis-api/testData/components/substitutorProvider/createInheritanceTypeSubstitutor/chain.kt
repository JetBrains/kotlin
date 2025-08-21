// WITH_STDLIB

class A<caret_base>A : BB<Int, String>()

open class BB<S, T> : CC<S, T, List<T>>()

open class C<caret_super>C<X, Y, Z> {
    context(_: kotlin.Triple<X, Y, Z>)
    fun Generic<X, Y, Z>.foo(): kotlin.Triple<X, Y, Z> = TODO()

    context(_: kotlin.Triple<X, Y, Z>)
    val Generic<X, Y, Z>.bar: kotlin.Triple<X, Y, Z> get() = TODO()
}

class Generic<A, B, C>