// IGNORE_BACKEND_FIR: JVM_IR
class A {
    operator fun component1() = "O"
    operator fun component2(): String = throw RuntimeException("fail 0")
    operator fun component3() = "K"
}

fun box(): String {
    val aA = Array(1) { A() }

    for ((x, _, z) in aA) {
        return x + z
    }

    return "OK"
}
