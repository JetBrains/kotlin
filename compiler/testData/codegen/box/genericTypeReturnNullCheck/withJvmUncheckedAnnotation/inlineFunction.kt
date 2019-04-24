// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

@file:UseExperimental(JvmUnchecked::class)

import kotlin.experimental.JvmUnchecked

inline fun <T> foo(): @JvmUnchecked T = null as T

fun box(): String {
    try {
        foo<String>()
    } catch (e: KotlinNullPointerException) {
        return "Fail: KotlinNullPointerException should not have been thrown"
    }
    return "OK"
}
