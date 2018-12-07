// SHOW_KOTLIN_VARIABLES

package frameInlineFunCallInsideInlineFunKotlinVariables

class A {
    inline fun inlineFun(s: (Int) -> Unit) {
        val element = 1.0
        //TODO breakpoint here doesn't work (not only in tests)
        s(1)
    }

    val prop = 1
}

class B {
    inline fun foo(s: (Int) -> Unit) {
        val element = 2
        val a = A()
        // STEP_INTO: 1
        // STEP_OVER: 1
        //Breakpoint!
        a.inlineFun {
            val e = element
        }
        s(1)
    }
}

class C {
    fun bar() {
        val element = 1f
        B().foo {
            val e = element
        }
    }
}

fun main(args: Array<String>) {
    C().bar()
}

// PRINT_FRAME

// EXPRESSION: element
// RESULT: 1.0: D

// EXPRESSION: this.prop
// RESULT: 1: I

