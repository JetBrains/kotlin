// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LATEST_LV_DIFFERENCE
// DISABLE_NEXT_TIER_SUGGESTION: we need to run fi2ir to get all actualization diagnostics

// MODULE: common

expect class A

expect class B

class <!CLASSIFIER_REDECLARATION!>C<!>

// MODULE: intermediate()()(common)

actual class <!CLASSIFIER_REDECLARATION!>A<!>

class <!ACTUAL_MISSING, ACTUAL_MISSING{METADATA}, CLASSIFIER_REDECLARATION!>B<!>

expect class C

// MODULE: main()()(common, intermediate)

class A

actual class <!ACTUAL_WITHOUT_EXPECT!>B<!>

actual class C
