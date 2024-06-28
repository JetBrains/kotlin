// TARGET_BACKEND: JVM

// Wrong function resolution after package renaming
// IGNORE_BACKEND: ANDROID

// WITH_STDLIB

// FILE: A.kt

package kotlin.internal

fun apiVersionIsAtLeast(epic: Int, major: Int, minor: Int): Boolean {
    return false
}

inline fun versionDependentInlineFun() = if (apiVersionIsAtLeast(1, 1, 0)) true else false
inline fun testInline() = versionDependentInlineFun()

fun testNonInline() = versionDependentInlineFun()

// FILE: B.kt
import kotlin.internal.*

fun box(): String {
    val clazz = Class.forName("kotlin.internal.AKt")
    val func = clazz.methods.single { it.name == "testInline" }
    if (func.invoke(null) as Boolean == true) return "Fail 1"

    if (!testNonInline()) return "Fail 2"

    return "OK"
}