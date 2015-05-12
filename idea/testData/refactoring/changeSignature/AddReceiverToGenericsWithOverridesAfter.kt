class U<A>

interface T<A> {
    fun U<A>.foofoofoo<B>(b: B): Int
}

abstract class T1<X> : T<U<X>> {
    override fun <B> U<U<X>>.foofoofoo(b: B): Int {
        throw UnsupportedOperationException()
    }
}

abstract class T2 : T1<String>() {
    override fun <C> U<U<String>>.foofoofoo(b: C): Int {
        throw UnsupportedOperationException()
    }
}