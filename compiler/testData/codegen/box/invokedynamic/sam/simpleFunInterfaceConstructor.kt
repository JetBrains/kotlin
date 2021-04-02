// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

fun interface KRunnable {
    fun run()
}

var test = "Failed"

fun box(): String {
    KRunnable { test = "OK" }.run()
    return test
}
