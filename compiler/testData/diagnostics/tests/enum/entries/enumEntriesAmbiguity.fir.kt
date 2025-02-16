// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +EnumEntries -PrioritizedEnumEntries -ForbidEnumEntryNamedEntries
// WITH_STDLIB
// FIR_DUMP

enum class Ambiguous {
    first, <!DECLARATION_OF_ENUM_ENTRY_ENTRIES_WARNING!>entries<!>;
}

val e = Ambiguous.entries.ordinal
