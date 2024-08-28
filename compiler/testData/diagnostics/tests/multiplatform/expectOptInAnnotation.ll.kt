// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@file:OptIn(ExperimentalMultiplatform::class)

expect annotation class ActualOnly

@RequiresOptIn
<!EXPECT_ACTUAL_OPT_IN_ANNOTATION!>expect<!> annotation class Both

@RequiresOptIn
@OptionalExpectation
expect annotation class MyOptIn

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@RequiresOptIn
actual annotation class <!ACTUAL_WITHOUT_EXPECT!>ActualOnly<!>

@RequiresOptIn
actual annotation class <!ACTUAL_WITHOUT_EXPECT!>Both<!>

@RequiresOptIn
actual annotation class MyOptIn
