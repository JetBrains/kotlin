// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// Functions can be recursively annotated
annotation class ann(val x: Int)
@ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>foo()<!>) fun foo() = 1

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, integerLiteral, primaryConstructor,
propertyDeclaration */
