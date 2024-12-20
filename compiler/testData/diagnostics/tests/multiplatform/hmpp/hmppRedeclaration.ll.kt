// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: common

class A

class C

// MODULE: intermediate()()(common)

class A

class B

// MODULE: main()()(common, intermediate)

class B

class C
