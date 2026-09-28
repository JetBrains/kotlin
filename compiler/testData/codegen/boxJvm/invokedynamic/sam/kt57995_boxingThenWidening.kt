// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// FULL_JDK
// SAM_CONVERSIONS: INDY
// LAMBDAS: CLASS

// CHECK_BYTECODE_TEXT
// 1 java/lang/invoke/LambdaMetafactory
// 0 \$sam\$

// LambdaMetafactory boxes an 'int' argument to 'Integer' and then widens it to any supertype of 'Integer', so
// converting a '(Number) -> Unit' reference to 'IntConsumer' is a valid adaptation and must use invokedynamic.

import java.util.function.IntConsumer

var result = 0

fun takeNumber(value: Number) {
    result = value.toInt()
}

fun box(): String {
    IntConsumer(::takeNumber).accept(42)
    return if (result == 42) "OK" else "Fail: $result"
}
