// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    assertEquals(java.lang.Integer.MIN_VALUE, Int.MIN_VALUE)
    assertEquals(java.lang.Byte.MAX_VALUE, Byte.MAX_VALUE)

/*
// TODO: uncomment when callable references to object members are supported
    assertEquals("MIN_VALUE", (Int.Companion::MIN_VALUE).name)
    assertEquals("MAX_VALUE", (Double.Companion::MAX_VALUE).name)
    assertEquals("MIN_VALUE", (Float.Companion::MIN_VALUE).name)
    assertEquals("MAX_VALUE", (Long.Companion::MAX_VALUE).name)
    assertEquals("MIN_VALUE", (Short.Companion::MIN_VALUE).name)
    assertEquals("MAX_VALUE", (Byte.Companion::MAX_VALUE).name)
*/

    return "OK"
}
