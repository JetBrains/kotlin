package multipleBreakpointsAtLine

fun main(args: Array<String>) {
    val a = A()

    // EXPRESSION: it + 1
    // RESULT: Unresolved reference: it
    //Breakpoint! (lambdaOrdinal = -1)
    a.foo(1) { 1 }.foo(2) { 1 }

    // EXPRESSION: it + 2
    // RESULT: 3: I
    //Breakpoint! (lambdaOrdinal = 1)
    a.foo(1) { 1 }.foo(2) { 1 }

    // EXPRESSION: it + 3
    // RESULT: 5: I
    //Breakpoint! (lambdaOrdinal = 2)
    a.foo(1) { 1 }.foo(2) { 1 }

    // EXPRESSION: it + 4
    // RESULT: Unresolved reference: it

    // EXPRESSION: it + 5
    // RESULT: 6: I

    // EXPRESSION: it + 6
    // RESULT: 8: I
    //Breakpoint!
    a.foo(1) { 1 }.foo(2) { 1 }

    // EXPRESSION: it + 7
    // RESULT: Unresolved reference: it

    // EXPRESSION: it + 8
    // RESULT: Unresolved reference: it
    //Breakpoint! (lambdaOrdinal = -1)
    a.bar(1) { 1 }.bar(2) { 1 }

    // EXPRESSION: it + 9
    // RESULT: 10: I
    //Breakpoint! (lambdaOrdinal = 1)
    a.bar(1) { 1 }.bar(2) { 1 }

    // EXPRESSION: it + 10
    // RESULT: 12: I
    //Breakpoint! (lambdaOrdinal = 2)
    a.bar(1) { 1 }.bar(2) { 1 }

    // EXPRESSION: it + 11
    // RESULT: Unresolved reference: it

    // EXPRESSION: it + 12
    // RESULT: 13: I

    // EXPRESSION: it + 13
    // RESULT: Unresolved reference: it

    // EXPRESSION: it + 14
    // RESULT: 16: I
    //Breakpoint!
    a.bar(1) { 1 }.bar(2) { 1 }

    // EXPRESSION: it + 15
    // RESULT: 17: I
    //Breakpoint! (lambdaOrdinal = 2)
    a.bar(1) { 1 }.bar(2) { 1 + 1
        1 + 1
    }
}

class A {
    fun foo(i: Int, f: (i: Int) -> Int): A {
        f(i)
        return this
    }

    inline fun bar(i: Int, f: (i: Int) -> Int): A {
        f(i)
        return this
    }
}
