// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// ISSUE: KT-87413

class ExplicitBackingFieldExtensionTarget

companion var ExplicitBackingFieldExtensionTarget.mutableValue: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>0<!>
    get() = <!UNRESOLVED_REFERENCE!>field<!>
    set(value) {
        <!UNRESOLVED_REFERENCE!>field<!> = value
    }

/* GENERATED_FIR_TAGS: assignment, getter, integerLiteral, propertyDeclaration, propertyWithExtensionReceiver, setter */
