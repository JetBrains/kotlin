package frameClassObject

fun main(args: Array<String>) {
    A().test()
}

class A {
    default object {
        val prop = 1
        fun myFun() = 1
    }

    fun test() {
        foo {
            //Breakpoint!
            prop
        }
    }
}

fun foo(f: () -> Unit) {
    f()
}

// PRINT_FRAME

// EXPRESSION: prop
// RESULT: 1: I

// EXPRESSION: myFun()
// RESULT: 1: I