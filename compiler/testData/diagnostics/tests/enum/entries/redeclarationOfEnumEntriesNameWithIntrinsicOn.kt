// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: -PrioritizedEnumEntries -ForbidEnumEntryNamedEntries
// WITH_STDLIB
// FIR_DUMP

enum class E {
    <!DECLARATION_OF_ENUM_ENTRY_ENTRIES_WARNING!>entries<!>, Entries;

    fun foo() {
        entries.ordinal
        E.entries.ordinal
    }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration */
