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

class MyThrowable : Throwable("")

fun box(): String {
    val i: I
    i = try {
        materialize()
    } catch(e: MyThrowable) {
        B()
    }
    return i.func()
}
