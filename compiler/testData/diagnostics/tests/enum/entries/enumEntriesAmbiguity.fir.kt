// !LANGUAGE: +EnumEntries -PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP

enum class Ambiguous {
    first, entries;
}

val e = Ambiguous.entries.ordinal
