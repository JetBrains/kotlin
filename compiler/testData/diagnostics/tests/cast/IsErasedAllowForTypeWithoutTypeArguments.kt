open class Base<A>
class Some: Base<Int>()

// No erased types in check
fun <A> f(a: Base<A>) = a is Some
