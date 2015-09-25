package stepOverWhenInReturn

fun main(args: Array<String>) {
    whenInReturn()
}

fun whenInReturn(): Int {
    val a = 1
    //Breakpoint!
    return when(a) {
        1 -> foo { 1 }
        else -> 3
    }
}

inline fun foo(f: () -> Int): Int {
    val a = 1
    return f()
}

fun test(i: Int) = 1

// STEP_OVER: 6