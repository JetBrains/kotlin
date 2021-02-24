// PROBLEM: none
class A {
    val a = 1

    <caret>inner class B {
        fun b() {
            a
        }
    }
}