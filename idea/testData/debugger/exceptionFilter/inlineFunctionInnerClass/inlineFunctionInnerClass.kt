package inlineFunctionInnerClass

fun box() {
    A().Inner().test()
}

class A {
    inner class Inner {
        fun test() {
            foo {}
        }
    }
}

// FILE: inlineFunctionFile.kt
// LINE: 4