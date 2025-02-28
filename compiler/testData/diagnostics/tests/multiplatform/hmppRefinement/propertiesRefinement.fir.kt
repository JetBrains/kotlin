// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect internal val foo: Int

// MODULE: intermediate1()()(common)
@OptIn(ExperimentalMultiplatform::class)
<!WRONG_ANNOTATION_TARGET!>@kotlin.experimental.ExpectRefinement<!>
expect internal val foo: Int

// MODULE: intermediate2()()(intermediate1)
@OptIn(ExperimentalMultiplatform::class)
<!WRONG_ANNOTATION_TARGET!>@kotlin.experimental.ExpectRefinement<!>
expect public val foo: Int

// MODULE: main()()(intermediate2)
actual public val foo: Int = 1
