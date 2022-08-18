// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// WITH_STDLIB

// MODULE: lib
// !LANGUAGE: +EnumEntries
// FILE: MyEnum.kt
enum class MyEnum {
    Nope, OK
}

// MODULE: caller(lib)
// !LANGUAGE: +EnumEntries
// FILE: Box.kt
@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    return MyEnum.entries[1].toString()
}
