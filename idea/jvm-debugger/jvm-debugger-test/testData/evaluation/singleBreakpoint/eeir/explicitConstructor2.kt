package explicitConstructor

class C() {
    constructor(a: Int) {
        //Breakpoint!
        40
    }
}

fun main(args: Array<String>) {
    C(1)
}


// EXPRESSION: a
// RESULT: 1: I
