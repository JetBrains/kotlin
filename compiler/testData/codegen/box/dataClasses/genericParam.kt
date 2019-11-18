// IGNORE_BACKEND_FIR: JVM_IR
data class A<T>(val x: T)

fun box(): String {
    val a = A(42)
    if (a.component1() != 42) return "Fail a: ${a.component1()}"
    
    val b = A(239.toLong())
    if (b.component1() != 239.toLong()) return "Fail b: ${b.component1()}"
    
    val c = A("OK")
    return c.component1()
}
