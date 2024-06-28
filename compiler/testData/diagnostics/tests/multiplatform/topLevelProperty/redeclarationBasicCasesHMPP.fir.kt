// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
expect val <!REDECLARATION!>x1<!>: Int

<!AMBIGUOUS_ACTUALS{JVM}!>expect val x2: Int<!>

<!AMBIGUOUS_ACTUALS{JVM}!>expect val x3: Int<!>

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
expect val x1: Int

val <!ACTUAL_MISSING, ACTUAL_MISSING{METADATA}, REDECLARATION!>x2<!> = 2

actual val <!REDECLARATION!>x3<!> = 3

// MODULE: main()()(intermediate)
<!AMBIGUOUS_EXPECTS!>actual val x1 = 1<!>

actual val x2 = 2

val <!ACTUAL_MISSING!>x3<!> = 3
