// "Change function signature to 'override fun <T> f(y: T, b: Int): T'" "true"
// ERROR: A 'return' expression required in a function with a block body ('{...}')
open class A {
    open fun <T> f(a: T, b: Int): T = a;
}

class B : A(){
    <caret>override fun <R> f(x: Double, y: R) {}
}
