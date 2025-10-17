// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
package test

annotation class Ann(
        val p1: Int,
        val p2: Int,
        val p3: Int
)

@Ann(1L.toInt().plus(1), 1.minus(1L.toInt()), 1L.toInt().times(1L.toInt())) class MyClass

// EXPECTED: @Ann(p1 = 2, p2 = 0, p3 = 1)

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, integerLiteral, primaryConstructor, propertyDeclaration */
