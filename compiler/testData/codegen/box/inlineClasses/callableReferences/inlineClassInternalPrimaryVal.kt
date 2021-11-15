// WITH_STDLIB
import kotlin.test.assertEquals

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(internal val x: Int)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class L(internal val x: Long)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class S(internal val x: String)

fun box(): String {
    assertEquals(42, Z::x.get(Z(42)))
    assertEquals(1234L, L::x.get(L(1234L)))
    assertEquals("abc", S::x.get(S("abc")))

    assertEquals(42, Z::x.invoke(Z(42)))
    assertEquals(1234L, L::x.invoke(L(1234L)))
    assertEquals("abc", S::x.invoke(S("abc")))

    return "OK"
}