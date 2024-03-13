// ISSUE: KT-65789, KT-58920
// LANGUAGE: +PrioritizedEnumEntries
// FIR_DUMP

enum class SomeClass {
    FIRST, LAST;

    class entries {
        companion object
    }
}

val resultEntries = SomeClass.entries
val resultEntriesRef = SomeClass::<!OVERLOAD_RESOLUTION_AMBIGUITY!>entries<!>
