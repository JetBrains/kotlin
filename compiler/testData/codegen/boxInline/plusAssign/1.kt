import test.*

fun test1(s: Int): Int {
    val z = Z(s)
    z += {s}
    return z.s
}

fun box(): String {
    val result = test1(11)
    if (result != 22) return "fail1: ${result}"

    return "OK"
}