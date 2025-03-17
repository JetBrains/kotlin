// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE

// MODULE: common

expect class A

expect class B

class <!CLASSIFIER_REDECLARATION!>C<!>

// MODULE: intermediate()()(common)

actual class <!CLASSIFIER_REDECLARATION!>A<!>

class <!ACTUAL_MISSING, ACTUAL_MISSING{METADATA}, CLASSIFIER_REDECLARATION!>B<!>

expect class C

// MODULE: main()()(common, intermediate)

class <!ACTUAL_MISSING!>A<!>

actual class B

actual class C
