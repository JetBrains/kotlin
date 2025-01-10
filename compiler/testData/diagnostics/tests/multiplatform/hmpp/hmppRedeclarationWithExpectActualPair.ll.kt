// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LATEST_LV_DIFFERENCE
// DISABLE_NEXT_PHASE_SUGGESTION: we need to run fi2ir to get all actualization diagnostics

// MODULE: common

expect class A

expect class B

class C

// MODULE: intermediate()()(common)

actual class A

class <!ACTUAL_MISSING!>B<!>

expect class C

// MODULE: main()()(common, intermediate)

class A

actual class <!ACTUAL_WITHOUT_EXPECT!>B<!>

actual class C
