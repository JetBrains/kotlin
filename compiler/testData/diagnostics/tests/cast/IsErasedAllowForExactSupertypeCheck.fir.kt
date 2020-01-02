open class Base<A>
class Some: Base<Int>()

// a is Some => a is Base<Int>
fun f(a: Some) = a is Base<Int>