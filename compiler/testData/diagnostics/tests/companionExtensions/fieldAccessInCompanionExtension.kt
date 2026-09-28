// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// ISSUE: KT-87413

class ExplicitBackingFieldExtensionTarget

companion var ExplicitBackingFieldExtensionTarget.mutableValue: Int = 0
    get() = field
    set(value) {
        field = value
    }

/* GENERATED_FIR_TAGS: assignment, getter, integerLiteral, propertyDeclaration, propertyWithExtensionReceiver, setter */
