// FIR_IDENTICAL
// ISSUE: KT-65789
// LANGUAGE: +PrioritizedEnumEntries
// FIR_DUMP

enum class SomeClass {
    FIRST, LAST;

    class entries {
        companion object
    }
}

val resultEntries = SomeClass.entries
val resultEntriesRef = SomeClass::entries
