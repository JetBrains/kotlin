package stepOverSimpleFun

fun main(args: Array<String>) {
    foo(1)
    val a = 1
}

fun foo(i: Int): Int {
    if (i > 0) {
        //Breakpoint!
        return 1
    }
    return 1
}

// Do not remove: kotlin strata should be available for this test
inline fun f(p: () -> Unit) {
    val b = 1
    p()
}

fun f2() {
    f {
        val b = 1
    }
}