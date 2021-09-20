interface I1 {
    fun o(): String
}
interface I2 {
    fun k(): String
}

interface I {
    fun foo(x: I1): I1
    fun foo(x: I2): I2
}

open class C : I {
    override fun foo(x: I1): I1 = x
    override fun foo(x: I2): I2 = x
}

class C2 : C() {
    override fun foo(x: I1): I1 = x
    override fun foo(x: I2): I2 = x
}

fun box(): String {
    val x: I = C2()

    val o = x.foo(object : I1 {
        override fun o(): String = "O"
    }).o()

    val k = x.foo(object : I2 {
        override fun k(): String = "K"
    }).k()

    return o + k
}
