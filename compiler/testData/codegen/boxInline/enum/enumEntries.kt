// IGNORE_BACKEND_MULTI_MODULE: JVM_MULTI_MODULE_OLD_AGAINST_IR
// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// !OPT_IN: kotlin.ExperimentalStdlibApi
// FILE: 1.kt

package test

import kotlin.enums.enumEntries

inline fun <reified T : Enum<T>> myEntries(): String {
    val values = enumEntries<T>()
    return values.joinToString("")
}

enum class Z {
    O, K
}


// FILE: 2.kt

import test.*

fun box(): String {
    return myEntries<Z>()
}
