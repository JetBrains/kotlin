// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>()

@Ann
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>MatchUseSameName<!>

@Ann
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>MatchUseTypealiasedName<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
annotation class AnnImpl
actual typealias Ann = AnnImpl

@Ann
actual class MatchUseSameName

@AnnImpl
actual class MatchUseTypealiasedName
