package isInsideInlineLambda

fun main(args: Array<String>) {
    val a = A()

    // EXPRESSION: it + 1
    // RESULT: 2: I
    //Breakpoint! (0)
    a.foo(1) { 1 }


    // inside other lambda
    a.foo(1) {
        // EXPRESSION: it + 2
        // RESULT: 3: I
        //Breakpoint! (0)
        a.foo(1) { 1 }
        1
    }

    // inside variable declaration
    // EXPRESSION: it + 3
    // RESULT: 4: I
    //Breakpoint! (0)
    val x = a.foo(1) { 1 }

    // inside object declaration
    val y = object {
        fun foo() {
            // EXPRESSION: it + 4
            // RESULT: 5: I
            //Breakpoint! (0)
            a.foo(1) { 1 }
        }
    }
    y.foo()

    // inside local function
    fun local() {
        // EXPRESSION: it + 5
        // RESULT: 6: I
        //Breakpoint! (0)
        a.foo(1) { 1 }
    }
    local()

    isInsideInlineLambdaInLibrary.test()
}

class A {
    inline fun foo(i: Int, f: (i: Int) -> Int): A {
        f(i)
        return this
    }
}

// ADDITIONAL_BREAKPOINT: isInsideInlineLambdaInLibrary.kt:Breakpoint1:(0)
// EXPRESSION: it + 11
// RESULT: 12: I

// ADDITIONAL_BREAKPOINT: isInsideInlineLambdaInLibrary.kt:Breakpoint2:(0)
// EXPRESSION: it + 12
// RESULT: 14: I

// ADDITIONAL_BREAKPOINT: isInsideInlineLambdaInLibrary.kt:Breakpoint3:(0)
// EXPRESSION: it + 13
// RESULT: 16: I

// ADDITIONAL_BREAKPOINT: isInsideInlineLambdaInLibrary.kt:Breakpoint4:(0)
// EXPRESSION: it + 14
// RESULT: 18: I

// ADDITIONAL_BREAKPOINT: isInsideInlineLambdaInLibrary.kt:Breakpoint5:(0)
// EXPRESSION: it + 15
// RESULT: 20: I
