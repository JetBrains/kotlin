package source

class <caret>A(val a: A, val a2: source.A) {
    val klass = javaClass<A>()
    val klass2 = javaClass<source.A>()
    val aa = A(a)
    val aa2 = source.A(a)
}

class B {

}