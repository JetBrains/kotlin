// TARGET_BACKEND: JVM
// WITH_STDLIB

@JvmInline
value class Z(val value: String)

fun <T : U, U : Z> foo(t: T) = t.value

fun box() = foo(Z("OK"))
