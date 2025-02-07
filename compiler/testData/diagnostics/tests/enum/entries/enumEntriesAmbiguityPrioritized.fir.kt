// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EnumEntries +PrioritizedEnumEntries -ForbidEnumEntryNamedEntries
// WITH_STDLIB
// FIR_DUMP

enum class Ambiguous {
    first, <!DECLARATION_OF_ENUM_ENTRY_ENTRIES_WARNING!>entries<!>;
}

val e = Ambiguous.<!OVERLOAD_RESOLUTION_AMBIGUITY!>entries<!>.<!UNRESOLVED_REFERENCE!>ordinal<!>
