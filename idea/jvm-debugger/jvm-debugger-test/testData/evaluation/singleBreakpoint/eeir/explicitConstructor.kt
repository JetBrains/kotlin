package explicitConstructor

class C() {
    // EXPRESSION: a
    // RESULT: 1: I
    //FunctionBreakpoint!
    constructor(a: Int) {

    }
}

fun main(args: Array<String>) {
    C(1)
}
