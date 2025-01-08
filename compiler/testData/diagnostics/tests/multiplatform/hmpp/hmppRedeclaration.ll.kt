// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_TIER_SUGGESTION: we need to run fi2ir to get all actualization diagnostics
// LATEST_LV_DIFFERENCE

// MODULE: common

class A

class C

// MODULE: intermediate()()(common)

class A

class B

// MODULE: main()()(common, intermediate)

class B

class C
