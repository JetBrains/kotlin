//ALLOW_AST_ACCESS
package test

open class Pair<T1, T2>
open class Triple<T1, T2, T3>

class Outer<X> {
    inner class InnerTest1<A>: Pair<X, A>()
    typealias Test1<A> = Pair<X, A>

    class Nested<Y> {
        typealias Test2<B> = Pair<Y, B>
    }

    inner class Inner<Z> {
        typealias Test3<C> = Triple<X, Z, C>
    }
}
