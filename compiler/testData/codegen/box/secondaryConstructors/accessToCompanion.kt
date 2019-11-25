// IGNORE_BACKEND_FIR: JVM_IR
internal class A(val result: Int) {
    companion object {
        fun foo(): Int = 1
        val prop = 2
        val C = 3
    }

    constructor() : this(foo() + prop + C)
}

fun box(): String {
    val result = A().result
    if (result != 6) return "fail: $result"
    return "OK"
}
