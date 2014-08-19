package frameInnerClass

fun main(args: Array<String>) {
    A().Inner().test()
}

class A {
    val prop1 = 1

    inner class Inner {
        val prop2 = 1

        fun test() {
            //Breakpoint!
            prop1 + prop2
        }
    }
}

// PRINT_FRAME

// EXPRESSION: prop1
// RESULT: 1: I

// EXPRESSION: prop2
// RESULT: 1: I