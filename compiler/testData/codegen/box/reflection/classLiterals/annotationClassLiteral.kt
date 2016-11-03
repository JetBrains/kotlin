// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import kotlin.test.assertEquals

fun box(): String {
    assertEquals("Deprecated", Deprecated::class.simpleName)

    return "OK"
}
