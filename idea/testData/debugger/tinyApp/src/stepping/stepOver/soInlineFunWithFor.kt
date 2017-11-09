package soInlineFunWithFor

fun main(args: Array<String>) {
    //Breakpoint!
    foo()

    val r = any1 { it > 2 }

    foo()
}

fun foo() {}

inline fun any1(predicate: (Int) -> Boolean) {
    for (i in 1..2) {
        if (predicate(i)) {
            return
        }
    }
}

// STEP_OVER: 5