// FIR_IDENTICAL

// MODULE: common
// TARGET_PLATFORM: Common
expect class A
expect class B

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
actual class B
expect class C

// MODULE: main()()(intermediate)
actual class A
actual class C
