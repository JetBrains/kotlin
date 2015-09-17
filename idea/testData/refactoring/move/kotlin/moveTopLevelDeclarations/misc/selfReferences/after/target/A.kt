package target

import source.B

class A(val a: A) {
    val klass = javaClass<A>()
    val klass2 = javaClass<B>()
    val aa = A(a)
}