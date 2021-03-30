// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY

fun box(): String {
    val ok = "OK"
    return { ok }()
}
