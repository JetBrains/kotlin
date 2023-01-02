// !LANGUAGE: -EnumEntries

enum class E {
    <!DEPRECATED_DECLARATION_OF_ENUM_ENTRY!>entries,<!> Entries;

    fun foo() {
        entries
        E.entries
    }
}