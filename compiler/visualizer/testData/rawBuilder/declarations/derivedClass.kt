open class Base<T>(val x: T)

//                             constructor Base<T>(T)
//                             │       Derived.<init>.x: T
//                             │       │
class Derived<T : Any>(x: T) : Base<T>(x)

//                                       constructor Derived<T : Any>(T)
//                                       │       create.x: T
//                                       │       │
fun <T : Any> create(x: T): Derived<T> = Derived(x)
