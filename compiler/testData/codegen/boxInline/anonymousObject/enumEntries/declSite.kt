// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// MODULE: lib
// !LANGUAGE: -EnumEntries
// FILE: 1.kt

package test

enum class X {
    O,
    K
}

// MODULE: caller(lib)
// !LANGUAGE: +EnumEntries

// FILE: 2.kt
import test.*

@OptIn(ExperimentalStdlibApi::class)
inline fun test(idx: Int): String {
    return X.entries[idx].toString()
}

fun box(): String {
    return test(0) + test(1)
}
