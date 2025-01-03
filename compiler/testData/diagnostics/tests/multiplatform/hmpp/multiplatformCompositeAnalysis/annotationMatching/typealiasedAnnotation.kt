// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Test<!>()

<!CONFLICTING_OVERLOADS!>@Test
expect fun unexpandedOnActual()<!>

<!CONFLICTING_OVERLOADS!>@Test
expect fun expandedOnActual()<!>

// MODULE: main()()(common)
annotation class JunitTestInLib

actual typealias <!AMBIGUOUS_EXPECTS!>Test<!> = JunitTestInLib

@Test
actual fun <!AMBIGUOUS_EXPECTS!>unexpandedOnActual<!>() {}

@JunitTestInLib
actual fun <!AMBIGUOUS_EXPECTS!>expandedOnActual<!>() {}

