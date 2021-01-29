// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY

var ok = "Failed"

fun box(): String {
    { ok = "OK" }()
    return ok
}
