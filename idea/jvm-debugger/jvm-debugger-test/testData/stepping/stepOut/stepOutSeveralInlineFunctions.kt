package stepOutSeveralInlineFunctions

fun main(args: Array<String>) {
    f1 {
        test(2)
    }
    test(3)
}

inline fun f1(f: () -> Unit) {
    //Breakpoint!
    val a = 1
    f2 {
        f()
    }
    val b = 2
}

inline fun f2(f: () -> Unit) {
    val a = 1
    f()
    val b = 2
}

fun test(i: Int) = 1