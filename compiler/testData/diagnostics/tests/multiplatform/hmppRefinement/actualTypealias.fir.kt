// LANGUAGE: +ExpectRefinement
// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect class Foo

// MODULE: intermediate1()()(common)
@kotlin.experimental.ExpectRefinement
expect class Foo

// MODULE: intermediate2()()(intermediate1)
@kotlin.experimental.ExpectRefinement
expect class Foo

// MODULE: main()()(intermediate2)
actual typealias Foo = Bar
class Bar
