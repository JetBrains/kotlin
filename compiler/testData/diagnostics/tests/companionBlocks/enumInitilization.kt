// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks
// ISSUE: KT-87255
// DUMP_CFG

enum class InitEnum(val text: String) {
    FIRST(<!UNINITIALIZED_ENUM_ENTRY!>SECOND<!>.name),
    SECOND(<!UNINITIALIZED_ENUM_COMPANION!>companionObject<!>),
    THIRD(companionBlock),
    FOURTH(companionBlockComputed),
    FIFTH(companionBlockFunction());

    companion object {
        val companionObject: String = FIRST.name
    }
    companion {
        val companionBlock: String = <!UNINITIALIZED_ENUM_ENTRY!>SECOND<!>.name
        val companionBlockComputed: String get() = SECOND.name
        fun companionBlockFunction() = SECOND.name
    }
}

/* GENERATED_FIR_TAGS: companionObject, enumDeclaration, enumEntry, objectDeclaration, primaryConstructor,
propertyDeclaration */
