package soInlineFunDex

fun main(args: Array<String>) {
    val a = 1

    //Breakpoint!
    simple()                           // 1

    withParam(1 + a)                   // 2

    withLambda { "hi" }                // 3
}                                      // 4

inline fun simple() {
    foo()
}

inline fun withParam(i: Int) {
}

inline fun withLambda(a: () -> Unit) {
    a()
}

fun foo() {}

// STEP_OVER: 6