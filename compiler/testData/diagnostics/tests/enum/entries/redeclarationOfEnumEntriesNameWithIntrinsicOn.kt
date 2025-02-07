// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +EnumEntries -PrioritizedEnumEntries -ForbidEnumEntryNamedEntries
// WITH_STDLIB
// FIR_DUMP

enum class E {
    <!DEPRECATED_DECLARATION_OF_ENUM_ENTRY!>entries,<!> Entries;

    fun foo() {
        entries.ordinal
        E.entries.ordinal
    }
}
