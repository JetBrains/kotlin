// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: common
// FILE: common.kt
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> class <!CLASSIFIER_REDECLARATION!>Foo<!>

// MODULE: main()()(common)
// FILE: test.kt
<!NO_ACTUAL_FOR_EXPECT!>expect<!> class Foo
