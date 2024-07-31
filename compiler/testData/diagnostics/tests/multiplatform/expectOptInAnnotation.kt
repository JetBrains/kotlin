// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
<!OPT_IN_WITHOUT_ARGUMENTS!>@file:OptIn(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>ExperimentalMultiplatform<!>::class<!>)<!>

expect annotation class ActualOnly

@RequiresOptIn
<!EXPECT_ACTUAL_OPT_IN_ANNOTATION{JVM}!>expect<!> annotation class Both

@RequiresOptIn
@<!UNRESOLVED_REFERENCE!>OptionalExpectation<!>
expect annotation class MyOptIn

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@RequiresOptIn
<!EXPECT_ACTUAL_OPT_IN_ANNOTATION!>actual<!> annotation class ActualOnly

@RequiresOptIn
<!EXPECT_ACTUAL_OPT_IN_ANNOTATION!>actual<!> annotation class Both

@RequiresOptIn
actual annotation class MyOptIn
