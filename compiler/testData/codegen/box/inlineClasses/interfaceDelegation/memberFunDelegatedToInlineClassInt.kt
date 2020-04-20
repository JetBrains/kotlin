// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR
import kotlin.test.assertEquals

interface IFoo {
    fun foo(s: String): String
}

inline class Z(val x: Int) : IFoo {
    override fun foo(s: String): String = x.toString() + s
}

class Test(x: Int) : IFoo by Z(x)

fun box(): String {
    assertEquals("1OK", Test(1).foo("OK"))

    return "OK"
}