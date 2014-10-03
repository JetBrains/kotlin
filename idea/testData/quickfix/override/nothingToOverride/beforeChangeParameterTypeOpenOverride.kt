// "Change function signature to 'fun f()'" "true"
open class A {
    open fun f() {}
}

open class B : A() {
    open override fun f() {}
}

class C : B() {
    <caret>override fun f(a : Int) {}
}
