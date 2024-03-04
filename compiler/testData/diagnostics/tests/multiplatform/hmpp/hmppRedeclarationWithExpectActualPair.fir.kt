// MODULE: common
// TARGET_PLATFORM: Common

expect class A

expect class B

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C<!>

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

actual class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!>

class <!ACTUAL_MISSING, ACTUAL_MISSING{METADATA}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!>

expect class C

// MODULE: main()()(common, intermediate)

class A

actual class <!ACTUAL_WITHOUT_EXPECT!>B<!>

actual class C
