open class Base<T>(val x: T)

class Derived<T : Any>(x: T) : Base<T>(x)

fun <T : Any> create(x: T): Derived<T> = Derived(x)