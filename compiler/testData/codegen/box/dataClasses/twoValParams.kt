// IGNORE_BACKEND_FIR: JVM_IR
data class A(val x: Int, val y: String)

fun box(): String {
    val a = A(42, "OK")
    return if (a.component1() == 42) a.component2() else a.component1().toString()
}
