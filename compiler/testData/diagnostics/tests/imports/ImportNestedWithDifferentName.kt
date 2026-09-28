// RUN_PIPELINE_TILL: CODEGEN
package a

import a.A.Nested as X

interface A {
    class Nested

    val a: Nested
    val b: X
}

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, nestedClass, propertyDeclaration */
