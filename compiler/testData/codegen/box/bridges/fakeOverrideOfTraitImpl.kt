var result = ""

trait D1 {
    fun foo(): D1 {
        result += "D1"
        return this
    }
}

trait F2 : D1

trait D3 : F2 {
    override fun foo(): D3 {
        result += "D3"
        return this
    }
}

class D4 : D3

fun box(): String {
    val x = D4()
    x.foo()
    (x : D3).foo()
    (x : F2).foo()
    (x : D1).foo()
    return if (result == "D3D3D3D3") "OK" else "Fail: $result"
}
