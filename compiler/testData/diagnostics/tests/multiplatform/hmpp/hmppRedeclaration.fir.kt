// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_TIER_SUGGESTION: we need to run fi2ir to get all actualization diagnostics
// LATEST_LV_DIFFERENCE

// MODULE: common

<!DUPLICATE_CLASS_NAMES!>class <!CLASSIFIER_REDECLARATION!>A<!><!>

<!DUPLICATE_CLASS_NAMES!>class <!CLASSIFIER_REDECLARATION!>C<!><!>

// MODULE: intermediate()()(common)

<!DUPLICATE_CLASS_NAMES!>class A<!>

<!DUPLICATE_CLASS_NAMES!>class <!CLASSIFIER_REDECLARATION!>B<!><!>

// MODULE: main()()(common, intermediate)

<!DUPLICATE_CLASS_NAMES!>class B<!>

<!DUPLICATE_CLASS_NAMES!>class C<!>
