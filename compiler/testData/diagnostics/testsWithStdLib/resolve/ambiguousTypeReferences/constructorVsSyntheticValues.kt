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

val resultValues = SomeClass.<!OVERLOAD_RESOLUTION_AMBIGUITY!>values<!>()
val resultValuesRef = SomeClass::<!OVERLOAD_RESOLUTION_AMBIGUITY!>values<!>
val resultEntries = SomeClass.entries
val resultEntriesRef = SomeClass::entries
