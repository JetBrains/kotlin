// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

var ok = "Failed"

fun box(): String {
    val lam = { ok = "OK" }
    lam()
    return ok
}
