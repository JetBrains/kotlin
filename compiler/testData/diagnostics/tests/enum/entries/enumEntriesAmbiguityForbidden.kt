// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EnumEntries +PrioritizedEnumEntries +ForbidEnumEntryNamedEntries
// WITH_STDLIB
// FIR_DUMP

enum class Ambiguous {
    first, <!DEPRECATED_DECLARATION_OF_ENUM_ENTRY!>entries;<!>
}

val e = <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>Ambiguous.entries<!NO_GET_METHOD!>[0]<!><!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>ordinal<!>
