// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true

open class Base<T> {
    open fun foo(x: T): Int = 42
}

open class Derived : Base<Int>() {
    override fun foo(x: Int) = x

    // override fun Base<Any?>.foo(x: Any?) = foo(<Int-unbox>(x as Int))
}

class Data(val s: String)

fun box(): String {
    val d = Derived()
    try {
        val x = (d as Base<Data>).foo(Data("zzz"))
        return "FAIL: $x"
    } catch (e: ClassCastException) {
        return "OK"
    }
}
