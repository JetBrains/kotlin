// PROBLEM: none
open class A {
    fun a() {}
}

open class B : A()

class C : B() {
    <caret>inner class D {
        fun d() {
            a()
        }
    }
}