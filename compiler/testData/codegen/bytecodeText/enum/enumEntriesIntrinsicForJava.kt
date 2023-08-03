// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// !OPT_IN: kotlin.ExperimentalStdlibApi
// FILE: MyEnum.java

enum MyEnum {
    E
}

// FILE: 1.kt

import kotlin.enums.*

inline fun <reified T : Enum<T>> enumEntries2(): EnumEntries<T> = enumEntries<T>()

fun foo() {
    enumEntries<MyEnum>()
    enumEntries2<MyEnum>()
}

// There should be one call to enumEntries in _1Kt$EntriesIntrinsicMappings.<clinit>.
// 2 GETSTATIC _1Kt\$EntriesIntrinsicMappings.entries\$0
// 1 INVOKESTATIC kotlin/enums/EnumEntriesKt.enumEntries
