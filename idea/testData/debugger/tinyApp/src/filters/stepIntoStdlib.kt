package stepIntoStdlib

fun main(args: Array<String>) {
    val a = intArray(1)
    //Breakpoint!
    a.withIndices()
    val b = 1
}

// STEP_INTO: 2
// TRACING_FILTERS_ENABLED: false
