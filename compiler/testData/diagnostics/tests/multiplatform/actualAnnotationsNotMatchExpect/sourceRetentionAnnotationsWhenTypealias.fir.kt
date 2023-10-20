// WITH_STDLIB
// DIAGNOSTICS: -ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION
// MODULE: m1-common
// FILE: common.kt
@Retention(AnnotationRetention.SOURCE)
annotation class Ann

@Ann
expect class SourceAvailable {
    @Ann
    fun foo()
}

@Ann
expect annotation class FromLib

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
class SourceAvailableImpl {
    fun foo() {}
}

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual typealias SourceAvailable = SourceAvailableImpl<!>

actual typealias FromLib = kotlin.SinceKotlin
