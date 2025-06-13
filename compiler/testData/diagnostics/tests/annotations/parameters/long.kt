// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
package test

annotation class Ann(
        val b1: Long,
        val b2: Long
)

@Ann(1, 1.toLong()) class MyClass

// EXPECTED: @Ann(b1 = 1.toLong(), b2 = 1.toLong())

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, integerLiteral, primaryConstructor, propertyDeclaration */
