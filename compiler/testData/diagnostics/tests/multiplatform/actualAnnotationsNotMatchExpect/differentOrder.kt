// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann1<!>
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann2<!>

@Ann1
@Ann2
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>AnnotationOrder<!>

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann3<!>(vararg val numbers: Int)

@Ann3(1, 2)
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ValuesOrderInsideAnnotationArgument<!>

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann4<!>(val arg1: String, val arg2: String)

<!CONFLICTING_OVERLOADS!>@Ann4(arg1 = "1", arg2 = "2")
expect fun differentArgumentsOrder()<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

@Ann2
@Ann1
actual class AnnotationOrder

@Ann3(2, 1)
actual class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>ValuesOrderInsideAnnotationArgument<!>

@Ann4(arg2 = "2", arg1 = "1")
actual fun differentArgumentsOrder() {}
