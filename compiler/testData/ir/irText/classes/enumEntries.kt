// !LANGUAGE: +EnumEntries
// TARGET_BACKEND: JVM_IR
// FULL_JDK
// WITH_STDLIB

enum class MyEnum {
    Ok, Nope
}

@OptIn(ExperimentalStdlibApi::class)
fun box() = MyEnum.entries
