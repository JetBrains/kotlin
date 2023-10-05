// WITH_STDLIB

class A<caret_base>A : BB<Int, String>()

open class BB<S, T> : CC<S, T, List<T>>()

open class C<caret_super>C<X, Y, Z> {
    fun foo(): kotlin.Triple<X, Y, Z> = TODO()
}

