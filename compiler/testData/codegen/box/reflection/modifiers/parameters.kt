// WITH_REFLECT

import kotlin.test.assertTrue
import kotlin.test.assertFalse

inline fun Unit.foo(
        crossinline crossinline: () -> Unit,
        noinline coroutine x: Unit.() -> Continuation<Unit>,
        vararg vararg: Unit
) {}

fun box(): String {
    val p = Unit::foo.parameters

    assertFalse(p[0].isNoinline)
    assertFalse(p[0].isCrossinline)
    assertFalse(p[0].isVararg)
    assertFalse(p[0].isCoroutine)

    assertFalse(p[1].isNoinline)
    assertTrue(p[1].isCrossinline)
    assertFalse(p[1].isVararg)
    assertFalse(p[1].isCoroutine)

    assertTrue(p[2].isNoinline)
    assertFalse(p[2].isCrossinline)
    assertFalse(p[2].isVararg)
    assertTrue(p[2].isCoroutine)

    assertFalse(p[3].isNoinline)
    assertFalse(p[3].isCrossinline)
    assertTrue(p[3].isVararg)
    assertFalse(p[3].isCoroutine)

    return "OK"
}
