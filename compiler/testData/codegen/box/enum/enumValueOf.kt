// WITH_STDLIB
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_PHASE: 2.2.0
// KT-78701: Fixed in 2.2.20-Beta2

import kotlin.test.assertSame
import kotlin.test.assertFailsWith
import kotlin.test.assertContains

enum class E { OK }

fun <T> id(x: T) = x

fun box(): String {
    assertSame(E.OK, E.valueOf("OK"))
    assertSame(E.OK, enumValueOf<E>("OK"))

    val e1 = assertFailsWith<IllegalArgumentException> { E.valueOf("NO") }
    assertContains(e1.message ?: "", "NO")
    val e2 = assertFailsWith<IllegalArgumentException> { enumValueOf<E>("NO") }
    assertContains(e2.message ?: "", "NO")

    return enumValueOf<E>(id("OK")).name
}
