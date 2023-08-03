// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// !OPT_IN: kotlin.ExperimentalStdlibApi
// FILE: test/Z.java

package test;

public enum Z {
    O, K
}

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

// FILE: 2.kt

import test.*

fun box(): String {
    return myEntries<Z>()
}
