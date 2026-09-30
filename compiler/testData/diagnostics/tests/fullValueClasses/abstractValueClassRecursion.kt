// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

abstract value class SelfAbstract(x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>SelfAbstract<!>)

sealed value class SelfSealed(x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>SelfSealed<!>)

abstract value class ViaFinal(x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Final<!>)

value class Final(val a: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>ViaFinal<!>, val i: Int)

value class Recursive(val r: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Recursive<!>, val i: Int)

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration, sealed, value */
