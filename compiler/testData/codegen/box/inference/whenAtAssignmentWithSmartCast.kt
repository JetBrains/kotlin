// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-77008

interface I

interface I2 : I {
    fun func(): String
}

class A : I2 {
    override fun func(): String = "OK"
}

class B : I2 {
    override fun func(): String ="Fail B"
}

fun <T : I2> materialize(): T {
    return A() as T
}

var b = true

fun box(): String {
    val i: I
    i = when (b) {
        true -> materialize()
        else -> B()
    }
    return i.func()
}
