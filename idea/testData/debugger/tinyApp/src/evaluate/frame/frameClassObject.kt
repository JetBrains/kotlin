package frameClassObject

fun main(args: Array<String>) {
    A().test()
}

class A {
    class object {
        val prop = 1
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