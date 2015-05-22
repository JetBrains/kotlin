package stepIntoFromInlineFun

class A()

fun main(args: Array<String>) {
    val a = A()
    a.test { it + 1 }
    val b = 1
}

inline fun A.test(l: (Int) -> Unit) {
    //Breakpoint!
    l(11)
}

// STEP_INTO: 1
// TRACING_FILTERS_ENABLED: false
