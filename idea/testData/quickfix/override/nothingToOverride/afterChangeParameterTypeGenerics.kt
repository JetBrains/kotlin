// "Change method signature to 'override fun <T>f(y: T, x: Int)'" "true"
open class A {
    open fun <T>f(a: T, b: Int) {}
}

class B : A(){
    <caret>override fun <T>f(y: T, x: Int) {}
}
