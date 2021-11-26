// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_TARGET: 1.8

val ua = 1234U
val ub = 5678U
val uc = 3456U
val u = ua * ub + uc

fun box(): String {
    val rem = u % ub
    if (rem != uc) throw AssertionError("$rem")

    return "OK"
}
