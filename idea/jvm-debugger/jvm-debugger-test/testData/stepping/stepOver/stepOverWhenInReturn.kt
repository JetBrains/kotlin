package stepOverWhenInReturn

fun main(args: Array<String>) {
    whenInReturn()                                        // 4?
}                                                         // 5

fun whenInReturn(): Int {
    val a = 1
    //Breakpoint!
    return when(a) {                                      // 1 3?
        1 -> foo { 1 }                                    // 2
        else -> 3
    }
}

inline fun foo(f: () -> Int): Int {
    val a = 15
    return f()
}

fun test(i: Int) = 42

// STEP_OVER: 6