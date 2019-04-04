// IGNORE_BACKEND: JVM

fun f(s: String) = "$s"

fun g(s: String?) = "$s"

// 1 valueOf
// 0 NEW java/lang/StringBuilder