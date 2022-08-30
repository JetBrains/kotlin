// IGNORE_BACKEND: JS, JVM
// WITH_STDLIB

// MODULE: lib
// FILE: MyEnum.kt
enum class MyEnum {
    Nope, OK
}

// MODULE: main(lib)
// !LANGUAGE: +EnumEntries
// FILE: main.kt
@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    return MyEnum.entries[1].toString()
}
