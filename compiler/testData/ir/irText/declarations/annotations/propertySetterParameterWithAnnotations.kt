// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

annotation class AnnParam

@setparam:AnnParam
var p: Int = 0

class C(@setparam:AnnParam var p: Int)
