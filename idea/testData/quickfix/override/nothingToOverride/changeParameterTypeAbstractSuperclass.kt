// "Change function signature to 'fun f(a: Int)'" "true"
abstract class A {
    abstract fun f(a: Int)
}

class B : A(){
    <caret>override fun f(a: String) {}
}
