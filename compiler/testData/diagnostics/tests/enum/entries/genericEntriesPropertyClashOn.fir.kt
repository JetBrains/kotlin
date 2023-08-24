// !LANGUAGE: +EnumEntries -PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP

package pckg

enum class A {
    ;

    companion object
}

val <T> T.entries: Int get() = 0

fun test() {
    <!DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY!>A.entries<!>
    A.Companion.entries
}
