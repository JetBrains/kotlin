// FIR_IDENTICAL
open class A
open class B: A()

open class Base<in T>
class SubBase: Base<A>()

// f is SubBase => f is Base<A> => (Base<Contravariant T>, B <: A) f is Base<B>
fun test(f: SubBase) = <!USELESS_IS_CHECK!>f is Base<B><!>