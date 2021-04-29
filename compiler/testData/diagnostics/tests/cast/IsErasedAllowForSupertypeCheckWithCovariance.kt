// FIR_IDENTICAL
open class A
open class B: A()

open class Base<out T>
class SubBase: Base<B>()

// f is SubBase => (SubBase <: Base<B>) f is Base<B> => (B <: A, Base<Covariant T> => SubBase <: Base<A>) f is Base<A>
fun test(f: SubBase) = <!USELESS_IS_CHECK!>f is Base<A><!>