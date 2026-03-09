// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions
enum class E {
    Entry;
}

companion fun E.foo() {
    Entry
    entries
    values()
    valueOf("Entry")
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, funWithExtensionReceiver, functionDeclaration, stringLiteral */
