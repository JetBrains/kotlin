// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EnumEntries +PrioritizedEnumEntries -ForbidEnumEntryNamedEntries
// WITH_STDLIB
// FIR_DUMP

enum class E {
    <!DECLARATION_OF_ENUM_ENTRY_ENTRIES_WARNING!>entries<!>, Entries;

    fun foo() {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>entries<!>.<!UNRESOLVED_REFERENCE!>ordinal<!>
        E.<!OVERLOAD_RESOLUTION_AMBIGUITY!>entries<!>.<!UNRESOLVED_REFERENCE!>ordinal<!>
    }
}
