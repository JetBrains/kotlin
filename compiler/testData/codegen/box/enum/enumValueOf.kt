// WITH_STDLIB

import kotlin.test.assertSame
import kotlin.test.assertFailsWith

enum class E { OK }

fun <T> id(x: T) = x

fun box(): String {
    assertSame(E.OK, E.valueOf("OK"))
    assertSame(E.OK, enumValueOf<E>("OK"))

    assertFailsWith<IllegalArgumentException> { E.valueOf("NO") }
    assertFailsWith<IllegalArgumentException> { enumValueOf<E>("NO") }

    return enumValueOf<E>(id("OK")).name
}
