// "Change method signature to 'override fun f(y: Int, x: String)'" "true"
open class A {
    open fun f(a: Int, b: String) {}
}

class B : A(){
    <caret>override fun f(y: Int, x: String) {}
}
