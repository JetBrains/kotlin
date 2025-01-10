// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LATEST_LV_DIFFERENCE
// DISABLE_NEXT_PHASE_SUGGESTION: we need to run fi2ir to get all actualization diagnostics

// MODULE: common

expect class A

expect class B

<!DUPLICATE_CLASS_NAMES!>class <!CLASSIFIER_REDECLARATION!>C<!><!>

// MODULE: intermediate()()(common)

<!DUPLICATE_CLASS_NAMES!>actual class <!CLASSIFIER_REDECLARATION!>A<!><!>

<!DUPLICATE_CLASS_NAMES!>class <!ACTUAL_MISSING, ACTUAL_MISSING{METADATA}, CLASSIFIER_REDECLARATION!>B<!><!>

expect class C

// MODULE: main()()(common, intermediate)

<!DUPLICATE_CLASS_NAMES!>class A<!>

<!DUPLICATE_CLASS_NAMES!>actual class <!ACTUAL_WITHOUT_EXPECT!>B<!><!>

<!DUPLICATE_CLASS_NAMES!>actual class C<!>
