// LANGUAGE: -EnumEntries -PrioritizedEnumEntries
// WITH_STDLIB
import MyEnum.entries

enum class MyEnum

val entries = "local str"

fun test() {
    <!DEPRECATED_ACCESS_TO_ENTRIES_PROPERTY!>entries<!>
}
