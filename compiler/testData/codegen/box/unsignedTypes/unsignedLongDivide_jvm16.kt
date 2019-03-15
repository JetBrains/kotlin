// KJS_WITH_FULL_RUNTIME
// JVM_TARGET: 1.6
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR

val ua = 1234UL
val ub = 5678UL
val uai = ua.toUInt()
val u = ua * ub

fun box(): String {
    val div = u / ua
    if (div != ub) throw AssertionError("$div")

    val divInt = u / uai
    if (div != ub) throw AssertionError("$div")

    return "OK"
}