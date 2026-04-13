// RUN_PIPELINE_TILL: FRONTEND
// SKIP_ERRORS_BEFORE

annotation class X(val value: Y, val y: Y)
annotation class Y()

@X(<!ANNOTATION_USED_AS_ANNOTATION_ARGUMENT!>@Y()<!><!SYNTAX!><!>, y = Y())
fun foo1() {
}
@X(<!ANNOTATION_USED_AS_ANNOTATION_ARGUMENT!>@Y()<!><!SYNTAX!><!>, y = <!ANNOTATION_USED_AS_ANNOTATION_ARGUMENT!>@Y()<!><!SYNTAX!><!>)
fun foo2() {
}

annotation class W(val value: Array<Y>)
@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Z()

@W(<!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Z()<!> arrayOf(<!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Z()<!> Y())) // ANNOTATION_ON_ANNOTATION_ARGUMENT
fun foo30() {
}
@W(<!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Z()<!> [<!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Z()<!> Y()]) // ANNOTATION_ON_ANNOTATION_ARGUMENT
fun foo31() {
}

@W(arrayOf(<!ANNOTATION_USED_AS_ANNOTATION_ARGUMENT!>@Y()<!><!SYNTAX!><!>)) // ANNOTATION_USED_AS_ANNOTATION_ARGUMENT
fun foo40() {
}
@W([<!ANNOTATION_USED_AS_ANNOTATION_ARGUMENT!>@Y()<!><!SYNTAX!><!>]) // ANNOTATION_USED_AS_ANNOTATION_ARGUMENT
fun foo41() {
}

@W(<!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Z()<!> [<!ANNOTATION_USED_AS_ANNOTATION_ARGUMENT!>@Y()<!><!SYNTAX!><!>]) // Both ANNOTATION_USED_AS_ANNOTATION_ARGUMENT and ANNOTATION_ON_ANNOTATION_ARGUMENT
fun foo50() {
}

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, primaryConstructor,
propertyDeclaration */
