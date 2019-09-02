// PROBLEM: none
class A {
    fun a() {}

    open inner class B(i: Int)

    <caret>inner class C(i: Int) : B(i)
}