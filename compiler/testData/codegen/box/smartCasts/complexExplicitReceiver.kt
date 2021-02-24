abstract class A {
    abstract fun o(): String
}

interface B {
    fun k(): String
}

fun <T> id(x: T): T = x

fun foo(a: A?): String {
    if (a is B) {
        return id(a).o() + a!!.k()
    }

    return "fail"
}

class Impl : A(), B {
    override fun o(): String = "O"
    override fun k(): String = "K"
}

fun box(): String {
    return foo(Impl())
}
