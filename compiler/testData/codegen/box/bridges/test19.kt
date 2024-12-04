// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true

open class Base<T> {
    open fun foo(x: T): String = "zzz"
}

open class Derived : Base<String>() {
    override fun foo(x: String) = x

    // override fun Base<Any?>.foo(x: Any?) = foo(x as String)
}

class Data(val x: Int)

fun box(): String {
    val d = Derived()
    try {
        val s = (d as Base<Data>).foo(Data(42))
        return "FAIL: ${s.length}"
    } catch (e: ClassCastException) {
        return "OK"
    }
}
