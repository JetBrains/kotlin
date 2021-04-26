// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

fun box(): String {
    val test = { i: Int -> i + 40 }(2)
    if (test != 42) return "Failed: test=$test"

    return "OK"
}