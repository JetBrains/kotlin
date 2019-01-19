// KJS_WITH_FULL_RUNTIME
// JVM_TARGET: 1.8
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR

val ua = 1234UL
val ub = 5678UL
val uc = 3456UL
val u = ua * ub + uc

fun box(): String {
    val rem = u % ub
    if (rem != uc) throw AssertionError("$rem")

    return "OK"
}