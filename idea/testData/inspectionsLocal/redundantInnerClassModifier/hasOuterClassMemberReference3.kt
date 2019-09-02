// PROBLEM: none
class A {
    fun a() {}

    inner class B {

        <caret>inner class C {
            fun c() {
                a()
            }
        }
    }
}