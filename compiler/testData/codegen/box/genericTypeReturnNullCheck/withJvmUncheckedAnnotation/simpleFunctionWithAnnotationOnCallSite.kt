// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

@file:UseExperimental(JvmUnchecked::class)

import kotlin.experimental.JvmUnchecked

fun <T> foo(): T = null as T

fun box(): String {
    try {
        foo<@JvmUnchecked String>()
    } catch (e: KotlinNullPointerException) {
        return "Fail: KotlinNullPointerException should not have been thrown"
    }
    return "OK"
}
