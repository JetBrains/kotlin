// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

fun lambdaIsSerializable(fn: () -> Unit) = fn is java.io.Serializable

fun box(): String {
    if (lambdaIsSerializable {})
        return "Failed: indy lambdas should not be serializable"
    return "OK"
}
