package source

class <caret>A(val a: A) {
    val klass = javaClass<A>()
    val klass2 = javaClass<source.B>()
    val aa = A(a)
}

class B {

}