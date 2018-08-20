package funFromOuterClassInLamdba

fun main(args: Array<String>) {
    Outer().Inner().test()
}

class Outer {
    fun foo() = 1

    inner class Inner {
        fun innerFun() = 1
        fun test() {
            fun f() = 1

            // outer is captured in lambda
            lambda {
                // EXPRESSION: foo() + 1
                // RESULT: 2: I
                //Breakpoint!
                val a = foo()
            }

            // outer isn't captured in lambda
            lambda {
                // EXPRESSION: foo() + 2
                // RESULT: java.lang.AssertionError : Cannot find local variable: name = this
                //Breakpoint!
                val a = 1
            }

            // inner is captured in lambda
            lambda {
                // EXPRESSION: foo() + 3
                // RESULT: 4: I
                //Breakpoint!
                val a = innerFun()
            }
        }
    }
}

fun lambda(f: () -> Unit) = f()