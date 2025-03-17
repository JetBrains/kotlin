// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE

// MODULE: common

expect class A

expect class B

class C

// MODULE: intermediate()()(common)

actual class A

class <!ACTUAL_MISSING!>B<!>

expect class C

// MODULE: main()()(common, intermediate)

class <!ACTUAL_MISSING!>A<!>

actual class B

actual class C
