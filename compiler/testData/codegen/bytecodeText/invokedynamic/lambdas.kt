// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY

fun test(): String {
    val lam = {
        val lamO = { "O" }
        val lamK = { "K" }
        lamO() + lamK()
    }
    return lam()
}

// 3 INVOKEDYNAMIC
// 0 class LambdasKt\$test\$
