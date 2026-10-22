// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE

annotation class Anno(val i: Boolean)

@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>['1']<!> == ['2'])
class MyClass

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, collectionLiteral, equalityExpression,
primaryConstructor, propertyDeclaration */
