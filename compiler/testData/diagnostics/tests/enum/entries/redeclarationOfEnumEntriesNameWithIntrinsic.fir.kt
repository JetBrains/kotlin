// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -EnumEntries -PrioritizedEnumEntries -ForbidEnumEntryNamedEntries
// WITH_STDLIB

enum class E {
    <!DECLARATION_OF_ENUM_ENTRY_ENTRIES_WARNING!>entries<!>, Entries;

    fun foo() {
        entries
        E.entries
    }
}