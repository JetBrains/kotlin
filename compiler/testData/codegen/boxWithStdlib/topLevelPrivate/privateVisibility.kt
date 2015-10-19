// FULL_JDK
package test

import java.lang.reflect.Modifier
import kotlin.test.assertTrue


private val prop = "O"

private fun test() = "K"

fun box(): String {
    val clazz = Class.forName("test.PrivateVisibilityKt")
    assertTrue(Modifier.isPrivate(clazz.getDeclaredMethod("test").modifiers), "Private top level function should be private")
    assertTrue(Modifier.isPrivate(clazz.getDeclaredField("prop").modifiers), "Backing field for private top level property should be private")

    return "OK"
}