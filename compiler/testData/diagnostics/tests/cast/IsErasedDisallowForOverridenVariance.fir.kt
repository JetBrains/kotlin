open class A
open class B: A()

open class Base<out T>
open class SubBase<T> : Base<T>()


fun ff(l: Base<B>) = l is SubBase<A>
