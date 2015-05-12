interface A
interface B: A
interface D

interface BaseSuper<out T>
interface BaseImpl: BaseSuper<D>
interface DerivedSuper<out S>: <!INCONSISTENT_TYPE_PARAMETER_VALUES!>BaseSuper<S>, BaseImpl<!>

fun test(t: BaseSuper<B>) = t is DerivedSuper<A>