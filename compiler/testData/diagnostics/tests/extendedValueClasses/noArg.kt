// LANGUAGE: +ValueClasses
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

abstract value class Abstract
sealed value class Sealed : Abstract()
<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>value<!> class Final : Sealed()
<!WRONG_MODIFIER_TARGET!>value<!> object Object : Sealed()

abstract value class Abstract1()
sealed value class Sealed1() : Abstract()
value class Final1<!VALUE_CLASS_EMPTY_CONSTRUCTOR!>()<!> : Sealed()
<!WRONG_MODIFIER_TARGET!>value<!> object Object1<!CONSTRUCTOR_IN_OBJECT!>()<!> : Sealed()

/* GENERATED_FIR_TAGS: classDeclaration, objectDeclaration, sealed, value */
