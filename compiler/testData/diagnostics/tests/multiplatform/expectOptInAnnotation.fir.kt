// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@file:OptIn(ExperimentalMultiplatform::class)

<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> annotation class ActualOnly

@RequiresOptIn
<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}, EXPECT_ACTUAL_OPT_IN_ANNOTATION!>expect<!> annotation class Both

@RequiresOptIn
@OptionalExpectation
expect annotation class MyOptIn

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@RequiresOptIn
actual annotation class <!EXPECT_ACTUAL_INCOMPATIBILITY_ILLEGAL_REQUIRES_OPT_IN!>ActualOnly<!>

@RequiresOptIn
actual annotation class <!EXPECT_ACTUAL_INCOMPATIBILITY_ILLEGAL_REQUIRES_OPT_IN!>Both<!>

@RequiresOptIn
actual annotation class MyOptIn
