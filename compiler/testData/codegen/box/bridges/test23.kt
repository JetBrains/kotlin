// IGNORE_NATIVE: optimizationMode=OPT

open class Foo(val x: Int)

abstract class Base<T> {
    abstract fun bar(x: T)
}

class Derived<T : Foo> : Base<T>() {
    override fun bar(x: T) { }
}

fun box(): String {
    val d = Derived<Foo>()
    try {
        val x = (d as Base<Any>).bar(Any())
        return "FAIL: $x"
    } catch (e: ClassCastException) {
        return "OK"
    }
}
