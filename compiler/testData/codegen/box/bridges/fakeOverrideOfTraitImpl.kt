// IGNORE_BACKEND_FIR: JVM_IR
var result = ""

interface D1 {
    fun foo(): D1 {
        result += "D1"
        return this
    }
}

interface F2 : D1

interface D3 : F2 {
    override fun foo(): D3 {
        result += "D3"
        return this
    }
}

class D4 : D3

fun box(): String {
    val x = D4()
    x.foo()
    val d3: D3 = x
    val f2: F2 = x
    val d1: D1 = x
    d3.foo()
    f2.foo()
    d1.foo()
    return if (result == "D3D3D3D3") "OK" else "Fail: $result"
}
