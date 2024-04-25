// LANGUAGE: -EnumEntries -PrioritizedEnumEntries
// WITH_STDLIB
import MyEnum.entries

enum class MyEnum

val entries = "local str"

fun test() {
    entries
}
