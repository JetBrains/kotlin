// !LANGUAGE: +EnumEntries
// FIR_DUMP

enum class Ambiguous {
    first, <!DEPRECATED_DECLARATION_OF_ENUM_ENTRY!>entries;<!>
}

val e = Ambiguous.entries
