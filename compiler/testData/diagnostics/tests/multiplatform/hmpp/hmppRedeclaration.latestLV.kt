// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_PHASE_SUGGESTION: we need to run fi2ir to get all actualization diagnostics
// LATEST_LV_DIFFERENCE

// MODULE: common

class <!CLASSIFIER_REDECLARATION!>A<!>

class <!CLASSIFIER_REDECLARATION!>C<!>

// MODULE: intermediate()()(common)

class A

class <!CLASSIFIER_REDECLARATION!>B<!>

// MODULE: main()()(common, intermediate)

class B

class C
