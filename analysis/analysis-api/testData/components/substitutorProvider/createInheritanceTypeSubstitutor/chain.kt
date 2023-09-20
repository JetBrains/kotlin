// WITH_STDLIB

class A<caret_base>A : BB<Int, String>

class BB<S, T> : CC<S, T, List<T>>

class C<caret_super>C<X, Y, Z> {
    fun foo(): kotlin.Triple<X, Y, Z>
}

