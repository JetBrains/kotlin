// !LANGUAGE: +EnumEntries
// TARGET_BACKEND: JVM_IR
// FULL_JDK
// WITH_STDLIB

enum class MyEnum {
    OK, NOPE
}

@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    val entries = MyEnum.entries
    val entry = entries[0]
    return entry.toString()
}
