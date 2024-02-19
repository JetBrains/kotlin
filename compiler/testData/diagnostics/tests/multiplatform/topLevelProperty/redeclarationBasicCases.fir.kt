// LANGUAGE: +MultiPlatformProjects
// IGNORE_DIAGNOSTIC_API
// IGNORE_REVERSED_RESOLVE

// MODULE: common
// FILE: common.kt
expect val <!REDECLARATION!>x1<!>: Int
expect val <!REDECLARATION!>x1<!>: Int

expect val <!REDECLARATION!>x2<!>: Int
val <!REDECLARATION!>x2<!> = 2

<!AMBIGUOUS_ACTUALS{JVM}, NO_ACTUAL_FOR_EXPECT{JVM}!>expect val x3: Int<!>

// MODULE: jvm()()(common)
// FILE: main.kt
<!AMBIGUOUS_EXPECTS!>actual val x1 = 1<!>

actual val x2 = 2

actual val <!REDECLARATION!>x3<!> = 3
val <!ACTUAL_MISSING, REDECLARATION!>x3<!> = 3
