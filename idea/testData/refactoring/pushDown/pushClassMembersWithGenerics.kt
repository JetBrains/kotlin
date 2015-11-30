interface Z<T>

open class <caret>A<T> {
    // INFO: {"checked": "true"}
    val t1: T
    // INFO: {"checked": "true"}
    val t2: Z<T>

    // INFO: {"checked": "true"}
    fun <S> foo(t1: T, t2: Z<T>, s1: S, s2: Z<S>): Boolean = true

    // INFO: {"checked": "true"}
    inner class X : Z<T> {

    }

    // INFO: {"checked": "true"}
    class Y<U> : Z<U> {

    }
}

class B<S> : A<Z<S>>() {

}

class C<K> : A<B<K>>() {

}