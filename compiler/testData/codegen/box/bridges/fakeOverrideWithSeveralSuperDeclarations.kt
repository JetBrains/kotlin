// IGNORE_BACKEND_FIR: JVM_IR
interface D1 {
    fun foo(): Any
}

interface D2 {
    fun foo(): Number
}

interface F3 : D1, D2

open class D4 {
    fun foo(): Int = 42
}

class F5 : F3, D4()

fun box(): String {
    val z = F5()
    var result = z.foo()
    val d4: D4 = z
    val f3: F3 = z
    val d2: D2 = z
    val d1: D1 = z

    result += d4.foo()
    result += f3.foo() as Int
    result += d2.foo() as Int
    result += d1.foo() as Int
    return if (result == 5 * 42) "OK" else "Fail: $result"
}
