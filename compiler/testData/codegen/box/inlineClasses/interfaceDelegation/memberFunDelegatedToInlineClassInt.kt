// WITH_STDLIB
import kotlin.test.assertEquals

interface IFoo {
    fun foo(s: String): String
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val x: Int) : IFoo {
    override fun foo(s: String): String = x.toString() + s
}

class Test(x: Int) : IFoo by Z(x)

fun box(): String {
    assertEquals("1OK", Test(1).foo("OK"))

    return "OK"
}