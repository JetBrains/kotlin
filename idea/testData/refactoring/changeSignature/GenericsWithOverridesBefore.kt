class U<A>

interface T<A, B> {
    fun <C> <caret>foofoofoo(a: A, b: B, c: C): Int
}

abstract class T1<X, Y> : T<U<X>, U<Y>> {
    override fun <C> foofoofoo(a: U<X>, b: U<Y>, c: C): Int {
        throw UnsupportedOperationException()
    }
}

abstract class T2<X> : T1<X, String>() {
    override fun <C> foofoofoo(a: U<X>, b: U<String>, c: C): Int {
        throw UnsupportedOperationException()
    }
}

class T3 : T2<Any>() {
    override fun <D> foofoofoo(a: U<Any>, b: U<String>, c: D): Int {
        throw UnsupportedOperationException()
    }
}