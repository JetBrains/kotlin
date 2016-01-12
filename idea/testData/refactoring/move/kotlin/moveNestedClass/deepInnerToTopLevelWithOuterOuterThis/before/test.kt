package test

class A {
    inner class OuterOuterY

    inner class B {
        inner class <caret>C {
            fun test() {
                OuterOuterY()
                this@A.OuterOuterY()
            }
        }
    }
}