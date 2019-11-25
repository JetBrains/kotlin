// IGNORE_BACKEND_FIR: JVM_IR
interface I {
    fun foo(x: Int = 23): String
}

abstract class Base : I

class C : Base(), I {
    override fun foo(x: Int) = "C:$x"
}

fun box(): String {
    val x: I = C()
    val r = x.foo() + ";" + x.foo(42)
    if (r != "C:23;C:42") return "fail: $r"

    return "OK"
}