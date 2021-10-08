// WITH_RUNTIME
import kotlin.test.assertEquals

@JvmInline
value class Z(internal val x: Int)
@JvmInline
value class L(internal val x: Long)
@JvmInline
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