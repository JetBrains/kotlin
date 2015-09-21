package stepOverReifiedParam

fun main(args: Array<String>) {
    //Breakpoint!
    val a = 1
    foo(a)
    val b = 1
}

inline fun <reified T> foo(t: T): T {
    val a = t
    return a
}

// STEP_OVER: 3