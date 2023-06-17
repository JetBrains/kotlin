// TARGET_BACKEND: JVM

// Wrong function resolution after package renaming
// IGNORE_BACKEND: ANDROID

// WITH_STDLIB

// FILE: A.kt

package kotlin.internal
fun apiVersionIsAtLeast(epic: Int, major: Int, minor: Int): Boolean {
    return false
}
inline fun versionDependentInlineFun() = if (apiVersionIsAtLeast(1, 1, 0)) "Fail" else "OK"
inline fun test() = versionDependentInlineFun()

// FILE: B.kt
import kotlin.internal.*

fun box(): String {
    val clazz = Class.forName("kotlin.internal.AKt")
    val func = clazz.methods.single { it.name == "test" }
    return func.invoke(null) as String
}