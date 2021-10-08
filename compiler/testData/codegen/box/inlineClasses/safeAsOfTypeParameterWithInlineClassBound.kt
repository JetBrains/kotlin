// IGNORE_BACKEND: JVM
// WITH_RUNTIME

interface X
@JvmInline
value class Z(val value: Int) : X

fun <T> test(t: T) where T : X, T : Z = t as? Int

fun box(): String = if (test(Z(42)) != null) "fail" else "OK"
