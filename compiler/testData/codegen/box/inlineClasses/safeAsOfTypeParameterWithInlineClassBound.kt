// IGNORE_BACKEND: JVM

interface X
inline class Z(val value: Int) : X

fun <T> test(t: T) where T : X, T : Z = t as? Int

fun box(): String = if (test(Z(42)) != null) "fail" else "OK"
