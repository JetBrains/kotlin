// IGNORE_BACKEND_FIR: JVM_IR
interface Q {
    fun foo(a: Double): Double
}

interface Z {
    fun foo(a: Double = 1.0): Double
}

class R : Q, Z {
    override fun foo(a: Double) = a
}

fun box(): String {
    if (R().foo() != 1.0) return "fail 1"
    if (R().foo(2.0) != 2.0) return "fail 2"
    return "OK"
}
