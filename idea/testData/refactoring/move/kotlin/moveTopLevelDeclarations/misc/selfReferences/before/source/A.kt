package source

class <caret>A(val a: A) {
    val klass = javaClass<A>()
    val aa = A(a)
}

class B {

}