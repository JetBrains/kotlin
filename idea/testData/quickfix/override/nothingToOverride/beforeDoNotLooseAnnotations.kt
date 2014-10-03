// "Change function signature to 'override fun f(a: Int)'" "true"
annotation class annon

open class A {
    open fun f(a: Int) {}
}

class B : A(){
    annon <caret>override fun f(a: String) {}
}
