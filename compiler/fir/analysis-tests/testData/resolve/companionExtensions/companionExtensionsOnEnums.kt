// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
enum class E {
    Entry;
}

<!WRONG_MODIFIER_TARGET!>companion<!> fun E.foo() {
    Entry
    entries
    values()
    valueOf("Entry")
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, funWithExtensionReceiver, functionDeclaration, stringLiteral */
