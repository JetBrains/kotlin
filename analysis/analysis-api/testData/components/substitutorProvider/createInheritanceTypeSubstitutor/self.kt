// WITH_STDLIB

class A<caret_base>A<caret_super>A : BB<Int, String>

class BB<S, T> {
    fun foo(): Pair<S, T>
}
