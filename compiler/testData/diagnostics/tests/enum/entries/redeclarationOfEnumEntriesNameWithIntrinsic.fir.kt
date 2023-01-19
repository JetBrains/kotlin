// !LANGUAGE: -EnumEntries
// WITH_STDLIB

enum class E {
    entries, Entries;

    fun foo() {
        entries
        E.entries
    }
}