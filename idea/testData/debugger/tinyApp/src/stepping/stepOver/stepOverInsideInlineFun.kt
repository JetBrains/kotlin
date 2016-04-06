package stepOverInsideInlineFun

fun main(args: Array<String>) {
    bar {
        val b = 1
    }
}

inline fun bar(f: (Int) -> Unit) {
    //Breakpoint!
    val a = 1
    val f = f(1)
    val c = 1
}

// STEP_OVER: 3