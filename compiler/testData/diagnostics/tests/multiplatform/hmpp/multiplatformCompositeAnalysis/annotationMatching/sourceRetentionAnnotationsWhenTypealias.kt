// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// MODULE: common
expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>() // No @Retention SOURCE set

@Ann
expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>CommonVolatile<!>

// MODULE: main()()(common)
@Retention(AnnotationRetention.SOURCE)
actual annotation class <!AMBIGUOUS_EXPECTS!>Ann<!>

actual typealias <!AMBIGUOUS_EXPECTS!>CommonVolatile<!> = kotlin.jvm.Volatile
