// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

@file:UseExperimental(JvmUnchecked::class)

import kotlin.experimental.JvmUnchecked

fun map(): Map<String, @JvmUnchecked String?> = mapOf("x" to null)
val x: String by map()

fun box(): String {
    try {
        x
    } catch (e: KotlinNullPointerException) {
        return "Fail: KotlinNullPointerException should not have been thrown"
    }
    return "OK"
}
