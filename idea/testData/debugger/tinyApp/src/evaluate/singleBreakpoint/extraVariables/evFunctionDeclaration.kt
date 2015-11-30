package evFunctionDeclaration

class A(val a: Int) {
    //Breakpoint!
    fun foo() = a
}

fun main(args: Array<String>) {
    A(1).foo()
}

// PRINT_FRAME