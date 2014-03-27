trait D1 {
    fun foo(): Any
}

trait D2 {
    fun foo(): Number
}

trait F3 : D1, D2

open class D4 {
    fun foo(): Int = 42
}

class F5 : F3, D4()

fun box(): String {
    val z = F5()
    var result = z.foo()
    result += (z : D4).foo()
    result += (z : F3).foo() as Int
    result += (z : D2).foo() as Int
    result += (z : D1).foo() as Int
    return if (result == 5 * 42) "OK" else "Fail: $result"
}
