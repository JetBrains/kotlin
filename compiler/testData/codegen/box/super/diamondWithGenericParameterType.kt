interface Base<T> {
    fun f(t: T): String = "Fail: Base"
}

open class Left<T> : Base<T>

interface Right : Base<Number> {
    override fun f(t: Number): String = "OK"
}

open class Bottom : Left<Number>(), Right

class Z : Bottom() {
    fun g(): String = super.f(42)

    override fun f(t: Number): String = "Fail: Z"
}

fun box(): String = Z().g()
