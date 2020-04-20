// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR
import kotlin.test.assertEquals

interface IFoo {
    fun foo(s: String): String
}

inline class Z(val x: Long) : IFoo {
    override fun foo(s: String): String = x.toString() + s
}

class Test(x: Long) : IFoo by Z(x)

fun box(): String {
    assertEquals("1OK", Test(1L).foo("OK"))

    return "OK"
}