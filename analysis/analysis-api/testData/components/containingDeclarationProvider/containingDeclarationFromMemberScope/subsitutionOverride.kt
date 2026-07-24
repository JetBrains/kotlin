abstract class X<T> {
    context(c: T)
    fun <S> foo1(t: T): T = t

    context(c: T)
    fun <Q> foo2(t: T) {}

    context(c: T)
    fun <U> T.foo3() {}
}

class Y<caret> : X<Int>() {}
