// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// LAMBDAS: INDY
// WITH_STDLIB
// FULL_JDK

// no stream api on Android
// IGNORE_BACKEND: ANDROID

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 2 java/lang/invoke/LambdaMetafactory

import java.util.stream.Collectors

fun box(): String {
    return listOf("o", "k")
        .stream()
        .map { it.toUpperCase() }
        .collect(Collectors.joining())
}
