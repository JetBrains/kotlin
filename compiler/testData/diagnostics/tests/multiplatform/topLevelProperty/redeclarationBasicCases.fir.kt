// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect val <!REDECLARATION, REDECLARATION{METADATA}!>x1<!>: Int
expect val <!REDECLARATION, REDECLARATION{METADATA}!>x1<!>: Int

expect val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{METADATA}!>x2<!>: Int
val <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{METADATA}, REDECLARATION!>x2<!> = 2

<!AMBIGUOUS_ACTUALS{JVM}!>expect val x3: Int<!>

// MODULE: jvm()()(common)
// FILE: main.kt
<!AMBIGUOUS_EXPECTS!>actual val x1 = 1<!>

actual val x2 = 2

actual val <!REDECLARATION!>x3<!> = 3
val <!ACTUAL_MISSING, REDECLARATION!>x3<!> = 3
