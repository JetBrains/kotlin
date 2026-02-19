// LANGUAGE: +EnumEntries
// TARGET_BACKEND: JVM_IR
// FULL_JDK
// WITH_STDLIB

// FILE: MyEnum.java
enum MyEnum {
    OK, NOPE
}

// FILE: test.kt
@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    val entries = MyEnum.entries
    val entry = entries[0]
    require(java.util.concurrent.TimeUnit.entries.size == java.util.concurrent.TimeUnit.values().size)
    return entry.toString()
}
