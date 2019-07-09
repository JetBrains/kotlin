// TARGET_BACKEND: JVM

// FULL_JDK
package test

import java.lang.reflect.Modifier

private val prop = "O"

private fun test() = "K"

fun box(): String {
    val clazz = Class.forName("test.PrivateVisibilityKt")
    if (!Modifier.isPrivate(clazz.getDeclaredMethod("test").modifiers))
        return "Private top level function should be private"
    if (!Modifier.isPrivate(clazz.getDeclaredField("prop").modifiers))
        return "Backing field for private top level property should be private"

    return "OK"
}
