package internalFunctionEvaluate

class SomeClass {
    internal fun f(): Int = 42
}

fun main(args: Array<String>) {
    val sc = SomeClass()
    //Breakpoint!
    val x = 12
}

// EXPRESSION: sc.f()
// RESULT: 42: I