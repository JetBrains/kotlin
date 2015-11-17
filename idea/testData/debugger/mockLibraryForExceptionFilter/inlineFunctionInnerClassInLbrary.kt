package inlineFunctionInnerClassInLbrary

fun box() {
    A().Inner().test()
}

class A {
    inner class Inner {
        fun test() {
            inlineFunInLibrary.foo {}
        }
    }
}