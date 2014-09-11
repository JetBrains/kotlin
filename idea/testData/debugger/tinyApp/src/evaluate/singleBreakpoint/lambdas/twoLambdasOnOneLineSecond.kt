package twoLambdasOnOneLineSecond

fun main(args: Array<String>) {
    val a = A()
    // EXPRESSION: it
    // RESULT: 2: I
    // STEP_INTO: 4
    //Breakpoint!
    a.foo { counter++; a }.foo { a }
}

var counter = 1

class A {
    fun foo(f: (Int) -> A): A {
        return f(counter)
    }
}