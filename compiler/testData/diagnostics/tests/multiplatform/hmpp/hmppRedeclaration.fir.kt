// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: common

class <!CLASSIFIER_REDECLARATION!>A<!>

class <!CLASSIFIER_REDECLARATION!>C<!>

// MODULE: intermediate()()(common)

class A

class <!CLASSIFIER_REDECLARATION!>B<!>

// MODULE: main()()(common, intermediate)

class B

class C
