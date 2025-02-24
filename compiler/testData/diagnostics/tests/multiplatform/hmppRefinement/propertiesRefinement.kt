// LANGUAGE: +ExpectRefinement
// FIR_IDENTICAL
// SKIP_K1
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect internal val foo: Int

// MODULE: intermediate1()()(common)
<!WRONG_ANNOTATION_TARGET!>@kotlin.experimental.ExperimentalExpectRefinement<!>
expect internal val foo: Int

// MODULE: intermediate2()()(intermediate1)
<!WRONG_ANNOTATION_TARGET!>@kotlin.experimental.ExperimentalExpectRefinement<!>
expect public val foo: Int

// MODULE: main()()(intermediate2)
actual public val foo: Int = 1
