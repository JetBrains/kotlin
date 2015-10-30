package isInsideInlineLambdaInLibrary

public fun test() {
    val a = A()
    //Breakpoint1
    a.foo(1) { 1 }

    // inside other lambda
    a.foo(100) {
        //Breakpoint2
        a.foo(2) { 1 }
        1
    }

    // inside variable declaration
    //Breakpoint3
    val x = a.foo(3) { 1 }

    // inside object declaration
    val y = object {
        fun foo() {
            //Breakpoint4
            a.foo(4) { 1 }
        }
    }
    y.foo()

    // inside local function
    fun local() {
        //Breakpoint5
        a.foo(5) { 1 }
    }
    local()
}

class A {
    inline fun foo(i: Int, f: (i: Int) -> Int): A {
        f(i)
        return this
    }
}
