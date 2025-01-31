// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: common
<!AMBIGUOUS_ACTUALS{JVM}!>expect<!> fun foo()
expect class Foo

// MODULE: intermediate()()(common)
actual <!CONFLICTING_OVERLOADS!>fun foo()<!> {}
actual class <!CLASSIFIER_REDECLARATION!>Foo<!>

// MODULE: main()()(common, intermediate)
actual fun foo() {}
actual class Foo
