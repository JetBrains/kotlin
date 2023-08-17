// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// WITH_RUNTIME

// MODULE: lib
// !LANGUAGE: -EnumEntries
// FILE: 1.kt

enum class X {
    O,
    K
}

// MODULE: caller(lib)
// !LANGUAGE: +EnumEntries

// FILE: F.kt

inline fun test(idx: Int, block: () -> String): String {
    return block()
}

@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    return test(0) { X.entries[0].toString() } +
            test(1) { X.entries[1].toString() }
}

// no additional mappings cause when in inline lambda (same module)
// 1 class FKt\$EntriesMappings
// 1 Lkotlin\/enums\/EnumEntries; entries\$0
// 0 Lkotlin\/enums\/EnumEntries; entries\$1
