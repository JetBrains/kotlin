// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import kotlin.test.assertTrue
import kotlin.test.assertFalse

inline fun Unit.foo(
        noinline coroutine x: Unit.() -> Continuation<Unit>,
        vararg vararg: Unit
) {}

fun box(): String {
    val p = Unit::foo.parameters

    assertFalse(p[0].isVararg)
    assertFalse(p[0].isCoroutine)

    assertFalse(p[1].isVararg)
    assertTrue(p[1].isCoroutine)

    assertTrue(p[2].isVararg)
    assertFalse(p[2].isCoroutine)

    return "OK"
}
