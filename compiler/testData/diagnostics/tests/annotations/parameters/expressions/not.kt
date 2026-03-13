// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions
// FIR_IDENTICAL
package test

annotation class Ann(
        val b1: Boolean,
        val b2: Boolean,
        val b3: Boolean
)

@Ann<!NO_VALUE_FOR_PARAMETER!>(!true, !false)<!> class MyClass

// EXPECTED: @Ann(b1 = false, b2 = true)

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, primaryConstructor, propertyDeclaration */
