// IGNORE_BACKEND_FIR: JVM_IR
open class A<T> {
    var size: T = 56 as T
}

interface C {
    var size: Int
}

class B : C, A<Int>()

fun box(): String {
    val b = B()
    if (b.size != 56) return "fail 1: ${b.size}"

    b.size = 55
    if (b.size != 55) return "fail 2: ${b.size}"

    return "OK"
}
