// TARGET_BACKEND: JVM
// API_VERSION: 1.4
// WITH_STDLIB
// MODULE: m1
// FILE: m1.kt

package kotlin

@SinceKotlin("1.7")
@kotlin.jvm.JvmName("bar")
@Suppress("CONFLICTING_OVERLOADS")
fun <T> List<T>.foo(): T = this[1]

// MODULE: m2
// FILE: m2.kt

package kotlin

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.5", hiddenSince = "1.6")
@Suppress("CONFLICTING_OVERLOADS")
fun <T> List<T>.foo(): T? = getOrNull(0)

// MODULE: m3(m1, m2)
// FILE: test.kt

fun box(): String = listOf("OK", "FAIL").foo()!!
