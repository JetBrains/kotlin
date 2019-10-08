package evFunctionDeclaration

class A(val a: Int) {
    //FunctionBreakpoint!
    fun foo() = a
}

fun main(args: Array<String>) {
    A(1).foo()
}

// PRINT_FRAME