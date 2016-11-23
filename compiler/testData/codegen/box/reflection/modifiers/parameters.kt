// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

inline fun Unit.foo(
        noinline coroutine x: Unit.() -> Continuation<Unit>,
        vararg vararg: Unit
) {}

fun box(): String {
    val p = Unit::foo.parameters

    assertEquals(2, p.size)

    assertFalse(p[0].isVararg)
    assertTrue(p[0].isCoroutine)

    assertTrue(p[1].isVararg)
    assertFalse(p[1].isCoroutine)

    return "OK"
}
