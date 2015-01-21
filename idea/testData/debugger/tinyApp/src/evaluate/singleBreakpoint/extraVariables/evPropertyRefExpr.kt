package evPropertyRefExpr

class A {
    var prop = 1

    fun test() {
        //Breakpoint!
        if (prop == 1) {

        }
    }
}

fun main(args: Array<String>) {
    A().test()
}

// PRINT_FRAME