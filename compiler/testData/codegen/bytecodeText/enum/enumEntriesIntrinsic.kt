// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// !OPT_IN: kotlin.ExperimentalStdlibApi

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE") // TODO: remove once KT-53154 is fixed.
import kotlin.enums.*

enum class MyEnum {
    E
}

inline fun <reified T : Enum<T>> enumEntries2(): EnumEntries<T> = enumEntries<T>()

fun foo() {
    enumEntries<MyEnum>()
    enumEntries2<MyEnum>()
}

// There should be one call to enumEntries in MyEnum.<clinit>.
// 1 INVOKESTATIC kotlin/enums/EnumEntriesKt.enumEntries
