// KJS_WITH_FULL_RUNTIME
// JVM_TARGET: 1.8
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR

val ua = 1234U
val ub = 5678U
val u = ua * ub

fun box(): String {
    val div = u / ua
    if (div != ub) throw AssertionError("$div")

    return "OK"
}
