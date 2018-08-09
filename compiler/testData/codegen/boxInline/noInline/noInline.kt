// FILE: 1.kt

inline fun calc(s: (Int) -> Int, noinline p: (Int) -> Int) : Int {
    val z = p
    return s(11) + z(11) + p(11)
}

inline fun extensionLambda(noinline bar: Int.() -> Int) = 10.bar()

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
fun test1(): Int {
    return calc( { l: Int -> 2*l},  { l: Int -> 4*l})
}

fun test2(): Int {
    return extensionLambda({this * 16})
}

fun box(): String {
    if (test1() != 110) return "test1: ${test1()}"
    if (test2() != 160) return "test2: ${test2()}"

    return "OK"
}
