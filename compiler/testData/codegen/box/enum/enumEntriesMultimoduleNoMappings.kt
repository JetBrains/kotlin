// IGNORE_BACKEND: JS, JVM
// WITH_STDLIB

// MODULE: lib
// !LANGUAGE: +EnumEntries
// FILE: MyEnum.kt
enum class MyEnum {
    Nope, OK
}

// MODULE: main(lib)
// !LANGUAGE: +EnumEntries
// FILE: Box.kt
@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    return MyEnum.entries[1].toString()
}
