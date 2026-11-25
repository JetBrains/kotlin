// RUN_PIPELINE_TILL: FRONTEND

annotation class Anno(val i: Boolean)

@Anno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, UNRESOLVED_REFERENCE!>['1']<!> == <!UNRESOLVED_REFERENCE!>['2']<!>)
class MyClass

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, collectionLiteral, equalityExpression,
primaryConstructor, propertyDeclaration */
