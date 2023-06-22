// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.TYPEALIAS, AnnotationTarget.CLASS)
annotation class Ann

@Ann
expect class KtTypealiasNotMatch

@Ann
expect class AnnotationsNotConsideredOnTypealias

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
class KtTypealiasNotMatchImpl

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>KtTypealiasNotMatch<!> = KtTypealiasNotMatchImpl<!>

class AnnotationsNotConsideredOnTypealiasImpl

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>@Ann
actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>AnnotationsNotConsideredOnTypealias<!> = AnnotationsNotConsideredOnTypealiasImpl<!>
