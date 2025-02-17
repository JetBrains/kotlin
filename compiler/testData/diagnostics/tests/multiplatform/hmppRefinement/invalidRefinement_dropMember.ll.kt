// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: common
expect class Foo {
    fun foo()
}

// MODULE: intermediate()()(common)
@ExperimentalExpectRefinement
expect class Foo

// MODULE: main()()(intermediate)
<!AMBIGUOUS_EXPECTS!>actual<!> class Foo
