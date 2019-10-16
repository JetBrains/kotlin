package stepOverInlineFunctionInReturn

fun main(args: Array<String>) {
    f()
}

fun f(): Int {
    //Breakpoint!
    val a = 1
    return foo {
        test(2)
    }
}

inline fun foo(f: () -> Int): Int {
    val a = 1
    return f()
}

fun test(i: Int) = 1

// STEP_OVER: 3