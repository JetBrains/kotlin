// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// DIAGNOSTICS: -DEBUG_INFO_MISSING_UNRESOLVED

annotation class X(val value: Array<Y>)
annotation class Y()
@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Z()

@X(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!WRONG_ANNOTATION_TARGET!>@Z()<!> []<!>)
fun foo0() {
}

@X(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!ANNOTATION_USED_AS_ANNOTATION_ARGUMENT!>@Z()<!> arrayOf()<!>)
fun foo1() {
}

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, primaryConstructor,
propertyDeclaration */
