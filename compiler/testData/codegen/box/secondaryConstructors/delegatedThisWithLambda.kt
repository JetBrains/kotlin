// IGNORE_BACKEND_FIR: JVM_IR
class A(val f: () -> Int) {
    constructor() : this({ 23 })
}

fun box(): String {
    val result = A().f()
    if (result != 23) return "fail: $result"
    return "OK"
}