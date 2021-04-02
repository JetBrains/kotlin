// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

fun interface KRunnable {
    fun run()
}

fun runIt(r: KRunnable) {
    r.run()
}

fun box(): String {
    var test = "Failed"
    runIt { test = "OK" }
    return test
}

