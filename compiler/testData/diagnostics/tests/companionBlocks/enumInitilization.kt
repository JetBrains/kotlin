// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// ISSUE: KT-87255
enum class InitEnum(val text: String) {
    FIRST(<!UNINITIALIZED_ENUM_ENTRY!>SECOND<!>.name),
    SECOND(<!UNINITIALIZED_ENUM_COMPANION!>companionObject<!>),
    THIRD(companionBlock);

    companion object {
        val companionObject: String = FIRST.name
    }
    companion {
        val companionBlock: String = <!UNINITIALIZED_ENUM_ENTRY!>SECOND<!>.name
    }
}

/* GENERATED_FIR_TAGS: companionObject, enumDeclaration, enumEntry, objectDeclaration, primaryConstructor,
propertyDeclaration */
