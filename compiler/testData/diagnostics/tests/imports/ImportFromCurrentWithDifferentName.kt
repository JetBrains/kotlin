// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
package a

import a.A as ER

interface A {
    val a: <!UNRESOLVED_REFERENCE!>A<!>
    val b: ER
}

/* GENERATED_FIR_TAGS: interfaceDeclaration, propertyDeclaration */
