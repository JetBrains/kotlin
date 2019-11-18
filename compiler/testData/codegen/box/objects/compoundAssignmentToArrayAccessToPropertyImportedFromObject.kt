// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

import Host.x

object Host {
    val x = intArrayOf(0)
}

fun box(): String {
    x[0] += 1
    if (x[0] != 1) return "Fail 1: ${x[0]}"

    x[0]++
    if (x[0] != 2) return "Fail 2: ${x[0]}"

    ++x[0]
    if (x[0] != 3) return "Fail 3: ${x[0]}"

    return "OK"
}