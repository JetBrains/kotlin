
inline class X(val x: Any?)

interface IFoo {
    fun foo(): X?
}

class Test : IFoo {
    override fun foo(): X = X(null)
}

fun box(): String {
    val t1: IFoo = Test()
    val x1 = t1.foo()
    if (x1 != X(null)) throw AssertionError("x1: $x1")

    val t2 = Test()
    val x2 = t2.foo()
    if (x2 != X(null)) throw AssertionError("x2: $x2")

    return "OK"
}