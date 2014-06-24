package frameExtensionFun

fun main(args: Array<String>) {
    A().foo()
}

class A {
    val prop = 1
}

fun A.foo() {
    //Breakpoint!
    prop
}

// PRINT_FRAME