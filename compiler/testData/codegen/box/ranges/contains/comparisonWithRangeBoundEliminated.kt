// IGNORE_BACKEND_FIR: JVM_IR
fun abs(x: Int) = if (x < 0) -x else x
fun abs(x: Long) = if (x < 0) -x else x

fun test1() =
        5 in abs(-1) .. 10

fun test2() =
        5 in 1 .. abs(-10)

fun test3() =
        5L in abs(-1L) .. 10L

fun test4() =
        5L in 1L .. abs(-10L)

fun box(): String {
    if (!test1()) return "Fail 1"
    if (!test2()) return "Fail 2"
    if (!test3()) return "Fail 3"
    if (!test4()) return "Fail 4"

    return "OK"
}