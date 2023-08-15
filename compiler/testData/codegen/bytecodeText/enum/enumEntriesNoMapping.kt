// TARGET_BACKEND: JVM_IR
// FULL_JDK
// WITH_STDLIB

// MODULE: lib
// !LANGUAGE: +EnumEntries
// FILE: MyEnums.kt
enum class MyEnum {
    N, O
}

enum class MyEnum2 {
    O, K
}

// MODULE: caller(lib)
// !LANGUAGE: +EnumEntries
// FILE: Box.kt

@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    return MyEnum.entries[1].toString() + MyEnum2.entries[1].toString()
}

// 0 class [a-zA-Z]+\$EntriesMappings
