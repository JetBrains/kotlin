// IGNORE_BACKEND_FIR: JVM_IR
class A {
    class Nested {
        val o = 111
        val k = 222
    }
    
    fun result() = (::Nested)().o + (A::Nested)().k
}

fun box(): String {
    val result = A().result()
    if (result != 333) return "Fail $result"
    return "OK"
}
