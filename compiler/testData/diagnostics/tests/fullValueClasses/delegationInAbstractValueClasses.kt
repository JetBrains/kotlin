// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

interface I {
    fun f(): Int
}

abstract value class Abstract(x: I) : <!VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION!>I<!> by x

sealed value class Sealed(x: I) : <!VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION!>I<!> by x

value class Final(val x: I) : I by x

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inheritanceDelegation, interfaceDeclaration,
primaryConstructor, propertyDeclaration, sealed, value */
