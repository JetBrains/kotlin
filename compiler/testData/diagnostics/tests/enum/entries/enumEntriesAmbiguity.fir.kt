// !LANGUAGE: +EnumEntries
// FIR_DUMP

enum class Ambiguous {
    first, entries;
}

val e = Ambiguous.entries
