package test

class A {
    companion object {
        fun Int.extFoo(n: Int) {}

        val Int.extBar: Int get() = 1
    }

    class <caret>B {
        fun test() {
            1.extFoo(1.extBar)
        }
    }
}