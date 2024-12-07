abstract class X<T> {
    fun <S> foo1(t: T): T = t
    fun <Q> foo2(t: T) {}
    fun <U> foo3() {}
}

class Y<caret> : X<Int> {}