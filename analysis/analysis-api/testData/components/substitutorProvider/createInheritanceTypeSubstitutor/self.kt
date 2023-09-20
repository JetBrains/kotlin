// WITH_STDLIB

class A<caret_base>A<caret_super>A : BB<Int, String>

class BB<S, T> : CC<S, T, List<T>> {
    fun foo(): Triple<S, T>
}
