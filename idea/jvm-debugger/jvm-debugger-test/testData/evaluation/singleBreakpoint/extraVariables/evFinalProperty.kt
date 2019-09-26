package evFinalProperty

class A {
    val prop = 1
}

fun main(args: Array<String>) {
    val a = A()
    //Breakpoint!
    val b = a.prop
}

// PRINT_FRAME