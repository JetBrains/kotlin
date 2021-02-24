
open class Base<T> {
    open fun f(x: T): String {
        return "Fail"
    }
}

abstract class Derived : Base<String>() {
    abstract override fun f(x: String): String
}

class Implementation : Derived() {
    override fun f(x: String): String {
        return x
    }
}

fun box(): String {
    val base = Implementation() as Base<String>
    return base.f("OK")
}
