// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EnumEntries +PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP

enum class E {
    <!DEPRECATED_DECLARATION_OF_ENUM_ENTRY!>entries<!>, Entries;

    fun foo() {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>entries<!>.<!UNRESOLVED_REFERENCE!>ordinal<!>
        E.<!OVERLOAD_RESOLUTION_AMBIGUITY!>entries<!>.<!UNRESOLVED_REFERENCE!>ordinal<!>
    }
}
