// MODULE: common
// TARGET_PLATFORM: Common

class <!CLASSIFIER_REDECLARATION!>A<!>

class <!CLASSIFIER_REDECLARATION!>C<!>

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

class A

class <!CLASSIFIER_REDECLARATION!>B<!>

// MODULE: main()()(common, intermediate)

class B

class C
