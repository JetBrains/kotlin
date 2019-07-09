open class Base<T1>(val x: T1)

class Derived<T2 : Any>(x: T2) : Base<T2>(x)

fun <T3 : Any> create(x: T3) /* Derived<T3> */ = Derived(x)