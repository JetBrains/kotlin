// FIR_IDENTICAL
open class Base<A>
class Some: Base<Int>()

// a is Some => a is Base<Int>
fun f(a: Some) = <!USELESS_IS_CHECK!>a is Base<Int><!>