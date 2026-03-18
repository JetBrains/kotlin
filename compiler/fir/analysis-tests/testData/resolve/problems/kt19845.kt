// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-19845
// WITH_STDLIB

// KT-19845: Incorrectly compiled spread operator in varargs inside annotation argument list

annotation class B(val i: Int)

annotation class A(vararg val bs: B)

@A(B(1), B(2), *arrayOf(B(4), B(5)), B(6))
class AnnotatedClass

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, collectionLiteral, integerLiteral, outProjection,
primaryConstructor, propertyDeclaration, vararg */
