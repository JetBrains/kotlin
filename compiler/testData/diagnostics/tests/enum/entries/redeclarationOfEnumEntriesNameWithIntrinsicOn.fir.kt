// !LANGUAGE: +EnumEntries -PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP

enum class E {
    entries, Entries;

    fun foo() {
        entries.ordinal
        E.entries.ordinal
    }
}