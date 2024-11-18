// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: common
// TARGET_PLATFORM: Common

class A

class C

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

class A

class B

// MODULE: main()()(common, intermediate)

class B

class C
