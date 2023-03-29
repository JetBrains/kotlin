// !LANGUAGE: +EnumEntries
// TARGET_BACKEND: JVM_IR
// FULL_JDK
// WITH_STDLIB

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57775, KT-57430, KT-57777

enum class MyEnum {
    Ok, Nope
}

@OptIn(ExperimentalStdlibApi::class)
fun box() = MyEnum.entries
