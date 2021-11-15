// WITH_STDLIB

val ua = 1234U
val ub = 5678U
val u = ua * ub

fun box(): String {
    val div = u / ua
    if (div != ub) throw AssertionError("$div")

    return "OK"
}
