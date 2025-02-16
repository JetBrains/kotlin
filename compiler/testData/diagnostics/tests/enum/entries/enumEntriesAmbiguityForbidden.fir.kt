// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EnumEntries +PrioritizedEnumEntries +ForbidEnumEntryNamedEntries
// WITH_STDLIB
// FIR_DUMP

enum class Ambiguous {
    first, <!DECLARATION_OF_ENUM_ENTRY_ENTRIES_ERROR!>entries<!>;
}

val e = Ambiguous.entries[0].ordinal
