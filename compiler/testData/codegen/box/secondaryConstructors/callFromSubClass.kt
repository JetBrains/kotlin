// IGNORE_BACKEND_FIR: JVM_IR
open class A(val x: String, val z: String) {
    constructor(z: String) : this("O", z)
}

class B(val y: String) : A("_")

fun box(): String {
    val b = B("K")
    val result = b.z + b.x + b.y
    if (result != "_OK") return "fail: $result"
    return "OK"
}