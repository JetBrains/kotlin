package frameExtFunExtFun

fun main(args: Array<String>) {
    Outer().run()
}

class A {
    val aProp = 1
    fun aMyFun() = 1
}

class Outer {
    val outerProp = 1
    fun outerMyFun() = 1

    fun A.foo() {
        val valFoo = 1
        class LocalClass {
            val lcProp = 1
            fun B.test() {
                val valTest = 1
                lambda {
                    //Breakpoint!
                    outerProp + aProp + lcProp + bProp + valFoo + valTest
                }
            }
            fun run() {
                B().test()
            }
        }

        LocalClass().run()
    }

    fun run() {
        A().foo()
    }
}

class B {
    val bProp = 1
    fun bMyFun() = 1
}

fun lambda(f: () -> Unit) {
    f()
}

// PRINT_FRAME

// EXPRESSION: valFoo
// RESULT: 1: I

// EXPRESSION: valTest
// RESULT: 1: I

// EXPRESSION: aProp
// RESULT: 1: I

// EXPRESSION: outerProp
// RESULT: 1: I

// EXPRESSION: bProp
// RESULT: 1: I

// EXPRESSION: aMyFun()
// RESULT: 1: I

// EXPRESSION: outerMyFun()
// RESULT: 1: I

// EXPRESSION: bMyFun()
// RESULT: 1: I