// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR

val ua = 1234U
val ub = 5678U
val uc = 3456U
val u = ua * ub + uc

fun box(): String {
    val rem = u % ub
    if (rem != uc) throw AssertionError("$rem")

    return "OK"
}
