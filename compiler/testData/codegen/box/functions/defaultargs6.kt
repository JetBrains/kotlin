// IGNORE_BACKEND_FIR: JVM_IR
interface A {
    fun foo(x: Int, y: Int = x + 20, z: Int = y * 2) = z
}

class B : A {}

fun box() = if (B().foo(1) == 42) "OK" else "Fail"
