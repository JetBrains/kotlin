package target

class A(val a: A, val a2: A) {
    val klass = javaClass<A>()
    val klass2 = javaClass<A>()
    val aa = A(a)
    val aa2 = A(a)
}