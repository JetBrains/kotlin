// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EnumEntries +PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP

enum class Ambiguous {
    first, <!DEPRECATED_DECLARATION_OF_ENUM_ENTRY!>entries<!>;
}

val e = Ambiguous.<!OVERLOAD_RESOLUTION_AMBIGUITY!>entries<!>.<!UNRESOLVED_REFERENCE!>ordinal<!>
