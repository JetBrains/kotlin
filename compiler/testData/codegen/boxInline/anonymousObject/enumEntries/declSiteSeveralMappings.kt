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

enum class Y {
    O,
    K
}

// MODULE: caller(lib)
// !LANGUAGE: +EnumEntries

// FILE: 2.kt
import test.*

@OptIn(ExperimentalStdlibApi::class)
fun funForAdditionalMappingArrayInMappingFile(): String = Y.entries[1].toString()

@OptIn(ExperimentalStdlibApi::class)
inline fun test(idx: Int): String = X.entries[idx].toString()

// FILE: 3.kt
import test.*

fun box(): String {
    return test(0) + test(1)
}
