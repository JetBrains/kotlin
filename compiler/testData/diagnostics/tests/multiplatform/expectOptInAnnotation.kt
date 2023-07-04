// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
expect annotation class ActualOnly

@RequiresOptIn
<!EXPECT_ACTUAL_OPT_IN_ANNOTATION{JVM}!>expect<!> annotation class Both

@OptIn(ExperimentalMultiplatform::class)
@RequiresOptIn
@OptionalExpectation
expect annotation class MyOptIn

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@RequiresOptIn
<!EXPECT_ACTUAL_OPT_IN_ANNOTATION!>actual<!> annotation class ActualOnly

@RequiresOptIn
<!EXPECT_ACTUAL_OPT_IN_ANNOTATION!>actual<!> annotation class Both

@RequiresOptIn
actual annotation class MyOptIn
