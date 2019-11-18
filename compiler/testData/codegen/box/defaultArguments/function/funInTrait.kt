// IGNORE_BACKEND_FIR: JVM_IR
interface Foo {
    fun foo(a: Double = 1.0): Double
}

class FooImpl : Foo {
    override fun foo(a: Double): Double {
        return a
    }
}
fun box(): String  {
    if (FooImpl().foo() != 1.0) return "fail"
    if (FooImpl().foo(2.0) != 2.0) return "fail"
    return "OK"
}