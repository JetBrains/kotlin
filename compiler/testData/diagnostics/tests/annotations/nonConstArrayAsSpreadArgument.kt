// RUN_PIPELINE_TILL: FRONTEND

enum class E { X }

@Repeatable
annotation class VarargEnumAnno(vararg val v: E)
@Repeatable
annotation class EnumArrayAnno(val v: Array<E>)

val x: Array<E> = arrayOf(E.X)

@VarargEnumAnno(v = <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>x<!>)
@VarargEnumAnno(*<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>x<!>)
@VarargEnumAnno(v = <!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION!>*<!><!ANNOTATION_ARGUMENT_MUST_BE_CONST!>x<!>)
@VarargEnumAnno(v = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>arrayOf(*<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>x<!>)<!>)
@EnumArrayAnno(v = <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>x<!>)
@EnumArrayAnno(v = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>arrayOf(*<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>x<!>)<!>)
@EnumArrayAnno(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>x<!>)
@EnumArrayAnno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>arrayOf(*<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>x<!>)<!>)
fun target(): String = "OK"

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, enumDeclaration, enumEntry, functionDeclaration,
outProjection, primaryConstructor, propertyDeclaration, stringLiteral, vararg */
