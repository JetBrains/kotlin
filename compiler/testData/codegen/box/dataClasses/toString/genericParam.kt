// IGNORE_BACKEND_FIR: JVM_IR
data class A<T>(val x: T)

fun box(): String {
    val a = A(42)
    if ("$a" != "A(x=42)") return "$a"
    
    val b = A(239.toLong())
    if ("$b" != "A(x=239)") return "$b"
    
    return "OK"
}
