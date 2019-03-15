// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    val x = (10L..50).map { it * 40L }
    assertEquals(400L, x.first())
    return "OK"
}
