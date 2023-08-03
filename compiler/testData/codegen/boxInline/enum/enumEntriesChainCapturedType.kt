// IGNORE_BACKEND_MULTI_MODULE: JVM_MULTI_MODULE_OLD_AGAINST_IR
// WITH_STDLIB
// !OPT_IN: kotlin.ExperimentalStdlibApi
// FILE: 1.kt

package test

import kotlin.enums.enumEntries

inline fun <reified Y : Enum<Y>> myEntries2(): String {
    val entries = { enumEntries<Y>() }.let { it() }
    return entries.joinToString("")
}

inline fun <reified T : Enum<T>> myEntries(): String {
    return myEntries2<T>()
}

enum class Z {
    O, K
}


// FILE: 2.kt

import test.*

fun box(): String {
    return myEntries<Z>()
}
