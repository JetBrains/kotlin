// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

// MODULE: common
expect class A
expect class B

// MODULE: intermediate()()(common)
actual class B
expect class C

// MODULE: main()()(intermediate)
actual class A
actual class C
