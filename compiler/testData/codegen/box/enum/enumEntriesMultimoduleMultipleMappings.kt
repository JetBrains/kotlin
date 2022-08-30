// IGNORE_BACKEND: JS, JVM
// WITH_STDLIB

// MODULE: lib
// FILE: MyEnums.kt
enum class MyEnum {
    N, O
}

enum class MyEnum2 {
    O, K
}

// MODULE: main(lib)
// !LANGUAGE: +EnumEntries
// FILE: main.kt

@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    return MyEnum.entries[1].toString() + MyEnum2.entries[1].toString()
}
