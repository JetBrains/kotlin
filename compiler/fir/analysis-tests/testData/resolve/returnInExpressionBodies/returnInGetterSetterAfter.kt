// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowReturnInExpressionBodyWithExplicitType, +ForbidReturnInExpressionBodyWithoutExplicitTypeEdgeCases, +ExplicitBackingFields
// DIAGNOSTICS: -REDUNDANT_RETURN
// ISSUE: KT-82297

val a1: Int
    get() = return 1

val a2
    get(): Int = return 1

var a3 = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>
    get() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> 1
    set(value) = return Unit

val a4 : Any
    field: Int = <!RETURN_NOT_ALLOWED!>return<!> 1

/* GENERATED_FIR_TAGS: explicitBackingField, getter, integerLiteral, propertyDeclaration, setter */
