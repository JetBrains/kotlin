// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// FULL_JDK
// SAM_CONVERSIONS: INDY
// LAMBDAS: CLASS

// CHECK_BYTECODE_TEXT
// 1 java/lang/invoke/LambdaMetafactory
// 0 \$sam\$
// 0 Kt57995_classLambdasKt\$box\$

import java.util.function.Consumer

var result = "Fail"

fun <T> foo(c: Consumer<in T>, value: T) {
    c.accept(value)
}

fun box(): String {
    foo<String>({ result = it }, "OK")
    return result
}
