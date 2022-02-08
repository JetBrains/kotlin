// WITH_STDLIB

import Host.x

object A {
    var xx = intArrayOf(0)
}

object Host {
    val A.x get() = A.xx
}

fun box(): String {
    A.x[0] += 1
    if (A.x[0] != 1) return "Fail 1: ${A.x[0]}"

    A.x[0]++
    if (A.x[0] != 2) return "Fail 2: ${A.x[0]}"

    ++A.x[0]
    if (A.x[0] != 3) return "Fail 3: ${A.x[0]}"

    return "OK"
}