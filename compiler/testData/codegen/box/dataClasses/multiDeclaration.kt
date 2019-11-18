// IGNORE_BACKEND_FIR: JVM_IR
data class A(val x: Int, val y: Any?, val z: String)

fun box(): String {
    val a = A(42, null, "OK")
    val (x, y, z) = a
    return if (x == 42 && y == null) z else "Fail"
}
