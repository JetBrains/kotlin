// RUN_PIPELINE_TILL: BACKEND
package a

import a.A.Nested as X

interface A {
    class Nested

    val a: Nested
    val b: X
}

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, nestedClass, propertyDeclaration */
