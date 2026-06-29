// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// ISSUE: KT-87255
enum class InitEnum(val text: String) {
    FIRST(<!UNINITIALIZED_ENUM_ENTRY!>SECOND<!>.name),
    SECOND(<!UNINITIALIZED_ENUM_COMPANION!>companionObject<!>),
    THIRD(<!UNINITIALIZED_ENUM_COMPANION_BLOCK_MEMBER!>companionBlock<!>),
    FOURTH(companionBlockComputed),
    FIFTH(companionBlockFunction());

    companion object {
        val companionObject: String = FIRST.name
    }
    companion {
        val companionBlock: String = SECOND.name
        val companionBlockComputed: String get() = SECOND.name
        fun companionBlockFunction() = SECOND.name
    }
}

/* GENERATED_FIR_TAGS: companionObject, enumDeclaration, enumEntry, objectDeclaration, primaryConstructor,
propertyDeclaration */
