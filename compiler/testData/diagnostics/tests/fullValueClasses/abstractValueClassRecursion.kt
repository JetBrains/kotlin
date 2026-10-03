// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

abstract value class SelfAbstract(x: SelfAbstract)

sealed value class SelfSealed(x: SelfSealed)

abstract value class ViaFinal(x: Final)

value class Final(val a: ViaFinal, val i: Int)

value class Recursive(val r: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Recursive<!>, val i: Int)

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration, sealed, value */
