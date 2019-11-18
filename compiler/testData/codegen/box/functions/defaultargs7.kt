// IGNORE_BACKEND_FIR: JVM_IR
class A(val expected: Int) {
    fun foo(x: Int, y: Int = x + 20, z: Int = y * 2) = z == expected
}

fun box() = if (A(42).foo(1)) "OK" else "Fail"
