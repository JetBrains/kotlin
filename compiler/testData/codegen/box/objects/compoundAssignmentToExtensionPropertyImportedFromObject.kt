// IGNORE_BACKEND_FIR: JVM_IR
import Host.x

object A {
    var xx = 0
}

object Host {
    var A.x
        get() = A.xx
        set(v) { A.xx = v }
}

fun box(): String {
    A.x += 1
    if (A.x != 1) return "Fail 1: ${A.x}"

    A.x++
    if (A.x != 2) return "Fail 2: ${A.x}"

    ++A.x
    if (A.x != 3) return "Fail 3: ${A.x}"

    return "OK"
}