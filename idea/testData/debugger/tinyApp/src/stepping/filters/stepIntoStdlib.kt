package stepIntoStdlib

fun main(args: Array<String>) {
    val a = intArrayOf(1)
    //Breakpoint!
    a.withIndex()
    val b = 1
}

// STEP_INTO: 1
// TRACING_FILTERS_ENABLED: false
