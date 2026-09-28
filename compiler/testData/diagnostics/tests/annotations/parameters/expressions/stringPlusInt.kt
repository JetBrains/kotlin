// RUN_PIPELINE_TILL: CODEGEN
package test

annotation class Ann(val s1: String)

@Ann(s1 = "a" + 1) class MyClass

// EXPECTED: @Ann(s1 = "a1")

/* GENERATED_FIR_TAGS: additiveExpression, annotationDeclaration, classDeclaration, integerLiteral, primaryConstructor,
propertyDeclaration, stringLiteral */
