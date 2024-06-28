// WITH_STDLIB

class A<caret_base>A<T, S> : BB<List<T>, Map<S, Int>, S>()

open class B<caret_super>B<Q, R, T>  {
    fun foo(): kotlin.Triple<Q, R, T> = TODO()
}


