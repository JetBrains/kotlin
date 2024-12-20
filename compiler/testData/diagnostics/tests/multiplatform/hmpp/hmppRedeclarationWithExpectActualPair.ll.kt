// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
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
