// "Change function signature to 'override fun f(a: Int)'" "false"
// ERROR: 'f' overrides nothing
// ACTION: Remove 'override' modifier
open class A {
    open fun foo() {}
    fun f(a: Int) {}
}

class B : A(){
    <caret>override fun f(a: String) {}
}
