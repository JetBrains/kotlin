// RUN_PIPELINE_TILL: FRONTEND
open class A

interface B : <!DELEGATION_IN_INTERFACE, DELEGATION_NOT_TO_INTERFACE, INTERFACE_WITH_SUPERCLASS!>A<!> by <!UNRESOLVED_REFERENCE!>a<!> {
    val a: A
}

val test = A()

interface C : <!DELEGATION_IN_INTERFACE, DELEGATION_NOT_TO_INTERFACE, INTERFACE_WITH_SUPERCLASS!>A<!> by test

/* GENERATED_FIR_TAGS: classDeclaration, inheritanceDelegation, interfaceDeclaration, propertyDeclaration */
