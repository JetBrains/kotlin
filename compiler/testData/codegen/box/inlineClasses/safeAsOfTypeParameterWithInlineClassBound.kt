// IGNORE_BACKEND: JVM
// WITH_STDLIB

interface X
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val value: Int) : X

fun <T> test(t: T) where T : X, T : Z = t as? Int

fun box(): String = if (test(Z(42)) != null) "fail" else "OK"
