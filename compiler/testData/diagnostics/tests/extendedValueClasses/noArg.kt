// LANGUAGE: +ValueClasses
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

abstract value class Abstract
sealed value class Sealed : Abstract()
<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>value<!> class Final : Sealed()
<!WRONG_MODIFIER_TARGET!>value<!> object Object : Sealed()

/* GENERATED_FIR_TAGS: classDeclaration, objectDeclaration, sealed, value */
