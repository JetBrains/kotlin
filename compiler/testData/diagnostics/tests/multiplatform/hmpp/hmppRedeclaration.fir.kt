// MODULE: common
// TARGET_PLATFORM: Common

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!>

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C<!>

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

class A

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!>

// MODULE: main()()(common, intermediate)

class B

class C
