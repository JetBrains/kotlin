open class A
open class B: A()

open class Base<out T>
open class SubBase<T> : Base<T>()

// l is Base<+B> => if (l is SubBase<?>) l is SubBase<+B> => l is SubBase<+A>
fun ff(l: Base<B>) = l is SubBase<out A>
