trait A
trait B: A
trait D

trait BaseSuper<out T>
trait BaseImpl: BaseSuper<D>
trait DerivedSuper<out S>: <!INCONSISTENT_TYPE_PARAMETER_VALUES!>BaseSuper<S>, BaseImpl<!>

fun test(t: BaseSuper<B>) = t is DerivedSuper<A>