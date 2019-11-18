// IGNORE_BACKEND_FIR: JVM_IR
fun baz(i: Int) = i
fun <T> bar(x: T): T = x

fun nullableFun(): ((Int) -> Int)? = null

fun box(): String {
    val x1: (Int) -> Int = bar(if (true) ::baz else ::baz)
    val x2: (Int) -> Int = bar(nullableFun() ?: ::baz)
    val x3: (Int) -> Int = bar(::baz ?: ::baz)

    val i = 0
    val x4: (Int) -> Int = bar(when (i) {
                                   10 -> ::baz
                                   20 -> ::baz
                                   else -> ::baz
                               })

    val x5: (Int) -> Int = bar(::baz!!)

    if (x1(1) != 1) return "fail 1"
    if (x2(1) != 1) return "fail 2"
    if (x3(1) != 1) return "fail 3"
    if (x4(1) != 1) return "fail 4"
    if (x5(1) != 1) return "fail 5"

    if ((if (true) ::baz else ::baz)(1) != 1) return "fail 6"

    return "OK"
}