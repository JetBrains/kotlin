// "Change function signature to 'override fun f(a: Int)'" "true"
open class A {
    open fun f(a: Int) {}
}

class B : A(){
    <caret>override fun f(a: String) {}
}
