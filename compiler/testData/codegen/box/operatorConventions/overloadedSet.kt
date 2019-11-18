// IGNORE_BACKEND_FIR: JVM_IR
object A {
    operator fun get(i: Int) = 1
    operator fun set(i: Int, j: Int) {}
    operator fun set(i: Int, x: Any) { throw Exception() }
}

fun box(): String {
    A[0]++
    A[0] += 1
    return "OK"
}