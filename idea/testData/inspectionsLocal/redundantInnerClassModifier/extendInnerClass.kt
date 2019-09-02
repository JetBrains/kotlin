// PROBLEM: none
class A {
    fun a() {}

    open inner class B

    <caret>inner class C : B()
}