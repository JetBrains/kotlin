interface Base<T> {
    fun f(): T = "Fail: Base" as T
}

open class Left<T> : Base<T>

interface Right : Base<String> {
    override fun f(): String = "OK"
}

open class Bottom : Left<String>(), Right

class Z : Bottom() {
    fun g(): String = super.f()

    override fun f(): String = "Fail: Z"
}

fun box(): String = Z().g()
