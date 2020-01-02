interface A
interface B: A
interface D

interface BaseSuper<out T>
interface BaseImpl: BaseSuper<D>
interface DerivedSuper<out S>: BaseSuper<S>, BaseImpl

fun test(t: BaseSuper<B>) = t is DerivedSuper<A>