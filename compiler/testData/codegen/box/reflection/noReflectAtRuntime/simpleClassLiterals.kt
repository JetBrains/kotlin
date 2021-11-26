// IGNORE_BACKEND: NATIVE

// WITH_STDLIB

import kotlin.test.assertNotNull

class Klass

fun box(): String {
    assertNotNull(Int::class)
    assertNotNull(String::class)
    assertNotNull(Klass::class)
    assertNotNull(Error::class)

    return "OK"
}
