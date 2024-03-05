// ISSUE: KT-65789
// LANGUAGE: -PrioritizedEnumEntries
// FIR_DUMP

enum class SomeClass {
    FIRST, LAST;

    class values {
        companion object
    }
    class entries {
        companion object
    }
}

val resultValues = SomeClass.values()
val resultValuesRef = SomeClass::values
val resultEntries = <!DEPRECATED_ACCESS_TO_ENTRIES_AS_QUALIFIER!>SomeClass.entries<!>
val resultEntriesRef = SomeClass::<!DEPRECATED_ACCESS_TO_ENUM_ENTRY_PROPERTY_AS_REFERENCE!>entries<!>
