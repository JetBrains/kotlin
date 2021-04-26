// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY
// WITH_RUNTIME

// desugaring on Android
// IGNORE_BACKEND: ANDROID

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

fun lambdaToString(fn: () -> Unit) = fn.toString()

fun box(): String {
    val str = lambdaToString {}
    if (!str.startsWith("LambdaToStingKt"))
        return "Failed: indy lambda toString is inherited from java.lang.Object: $str"
    return "OK"
}
