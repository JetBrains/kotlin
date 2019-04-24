// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

@file:UseExperimental(JvmUnchecked::class)

import kotlin.experimental.JvmUnchecked

fun <T> bar(): () -> @JvmUnchecked T = { null as T }

fun box(): String {
    try {
        val x = bar<String>()
        val y = x()
    } catch (e: KotlinNullPointerException) {
        return "Fail: KotlinNullPointerException should not have been thrown"
    }
    return "OK"
}
