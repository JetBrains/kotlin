package stepOutSeveralInlineArgumentDeepest

fun main(args: Array<String>) {
    f1 {
        //Breakpoint!
        test(2)
    }
    test(3)
}

inline fun f1(f: () -> Int) {
    val a = 1
    f2 {
        f()
    }
    val b = 2
}

inline fun f2(f: () -> Int): Int {
    val a = 1
    f()
    return 2
}

fun test(i: Int) = 1

// STEP_OUT: 4