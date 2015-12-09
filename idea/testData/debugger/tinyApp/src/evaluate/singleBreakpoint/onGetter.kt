package onGetter

fun main(args: Array<String>) {
    val a = A(1)
    // EXPRESSION: prop
    // RESULT: 1: I
    // STEP_INTO: 1
    //Breakpoint!
    a.prop
}

class A(val prop: Int)