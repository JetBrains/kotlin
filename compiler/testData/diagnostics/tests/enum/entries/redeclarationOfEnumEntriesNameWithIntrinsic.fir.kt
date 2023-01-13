// !LANGUAGE: -EnumEntries

enum class E {
    entries, Entries;

    fun foo() {
        entries
        E.entries
    }
}