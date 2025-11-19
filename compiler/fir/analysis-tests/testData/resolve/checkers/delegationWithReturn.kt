// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82466

interface Base
class Derived: Base

class C(val k: Derived): Base by <!RETURN_NOT_ALLOWED!>return<!> k

class D(val k: Derived): Base by <!TYPE_MISMATCH!>k <!UNRESOLVED_REFERENCE!><<!> <!RETURN_NOT_ALLOWED!>return<!> k<!>

/* GENERATED_FIR_TAGS: classDeclaration, inheritanceDelegation, interfaceDeclaration, primaryConstructor,
propertyDeclaration */
