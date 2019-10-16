package oneLineLambda

fun main(args: Array<String>) {
    val a = A()
    // EXPRESSION: it
    // RESULT: 1: I
    // STEP_INTO: 2
    //Breakpoint!
    a.foo { a }
}

class A {
    fun foo(f: (Int) -> A): A {
        return f(1)
    }
}