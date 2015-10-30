package a.b.c

class Outer {
    class Nested {
        inner class NI
        class NN
    }
    inner class Inner {
        inner class II
    }

    fun o() = Outer()
    fun n() = Nested()
    fun i() = Inner()
    fun II() = Inner().II()
    fun NI() = Nested().NI()
    fun NN() = Nested.NN()
}
