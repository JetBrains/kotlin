// FIR_IDENTICAL
// MODULE: common
// TARGET_PLATFORM: Common

class Foo

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

class Foo

// MODULE: main()()(common, intermediate)