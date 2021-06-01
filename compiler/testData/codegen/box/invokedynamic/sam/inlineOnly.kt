// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_RUNTIME
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory

import java.util.function.Consumer

fun call(c: Consumer<String>) {
}

fun box(): String {
    // println is inline only and therefore we cannot use invoke-dynamic to target it.
    call(::println)
    return "OK"
}
