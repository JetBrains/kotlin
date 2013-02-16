// "Specify Supertype Explicitly" "true"
// ERROR: Not a supertype
trait A {
    open fun f(){}
}

trait B {
    open fun f(){}
}

class C : A, B {
    override fun f() {
        super<<selection>...</selection><caret>>.f();
    }
}