// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -MultiplatformRestrictions
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
<!OPT_IN_WITHOUT_ARGUMENTS!>@file:OptIn(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>ExperimentalMultiplatform<!>::class<!>)<!>

expect annotation class ActualOnly

@RequiresOptIn
expect annotation class Both

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@RequiresOptIn
actual annotation class ActualOnly

@RequiresOptIn
actual annotation class Both
