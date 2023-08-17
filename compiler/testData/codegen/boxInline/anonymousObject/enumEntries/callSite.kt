// TARGET_BACKEND: JVM_IR
// NO_CHECK_LAMBDA_INLINING

// WITH_STDLIB

// MODULE: lib
// !LANGUAGE: -EnumEntries
// FILE: MyEnum.kt

package test

enum class X {
    O,
    K
}

inline fun test(block: () -> String): String {
    return block()
}

// MODULE: caller(lib)
// !LANGUAGE: +EnumEntries

// FILE: 2.kt

import test.*

@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    return test {
        X.entries[0].toString()
    } + test {
        X.entries[1].toString()
    }
}
