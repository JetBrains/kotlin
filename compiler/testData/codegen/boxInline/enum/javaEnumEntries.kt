// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_MULTI_MODULE: JVM_MULTI_MODULE_IR_AGAINST_OLD
// NO_CHECK_LAMBDA_INLINING
// OPT_IN: kotlin.ExperimentalStdlibApi
// WITH_STDLIB
// FILE: test/Z.java

package test;

public enum Z {
    O, K
}

// FILE: 1.kt

package test

import kotlin.enums.enumEntries

inline fun <reified T : Enum<T>> myEntries(): String {
    val values = enumEntries<T>()
    return values.joinToString("")
}

// FILE: 2.kt

import test.*

fun box(): String {
    return myEntries<Z>()
}
