package stepOverInsideInlineFun

fun main(args: Array<String>) {
    bar {
        val b = 1
    }
}

inline fun bar(f: (Int) -> Unit) {
    //Breakpoint!
    val a = 1
    foo()
    val f = f(1)
    val c = 1
}

fun foo() {}

// STEP_OVER: 5