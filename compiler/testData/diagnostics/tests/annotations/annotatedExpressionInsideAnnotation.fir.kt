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

@W(<!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Z()<!> arrayOf(@Z() Y())) // ANNOTATION_ON_ANNOTATION_ARGUMENT
fun foo30() {
}
@W(<!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@Z()<!> [@Z() Y()]) // ANNOTATION_ON_ANNOTATION_ARGUMENT
fun foo31() {
}

@W(arrayOf(@Y()<!SYNTAX!><!>)) // ANNOTATION_USED_AS_ANNOTATION_ARGUMENT
fun foo40() {
}
@W([@Y()<!SYNTAX!><!>]) // ANNOTATION_USED_AS_ANNOTATION_ARGUMENT
fun foo41() {
}

@W(@Z() [@Y()<!SYNTAX!><!>]) // Both ANNOTATION_USED_AS_ANNOTATION_ARGUMENT and ANNOTATION_ON_ANNOTATION_ARGUMENT
fun foo50() {
}
