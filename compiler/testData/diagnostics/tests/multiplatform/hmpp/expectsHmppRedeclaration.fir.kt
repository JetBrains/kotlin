// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect <!CONFLICTING_OVERLOADS!>fun foo()<!>
expect class Foo

// MODULE: intermediate()()(common)
expect fun foo()
expect class Foo

// MODULE: main()()(intermediate)
<!AMBIGUOUS_EXPECTS!>actual<!> fun foo() {}
actual class Foo
