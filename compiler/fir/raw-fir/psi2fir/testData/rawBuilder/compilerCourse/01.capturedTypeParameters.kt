class Base<T1> {
    fun <U> foo(x: T1): U = null!!

    inner class Inner<T2> {
        fun <R> bar(x: T1, y: T2): R = null!!
    }

    class Nested<T3> {
        fun <K> baz(x: T1, y: T3): K = null!!
    }

    fun functionWithLocals() {
        fun <U> localFoo(x: T1): U = null!!

        class Local<T4> {
            fun <E> localBar(x: T1, y: T4): E = null!!
        }
    }
}
