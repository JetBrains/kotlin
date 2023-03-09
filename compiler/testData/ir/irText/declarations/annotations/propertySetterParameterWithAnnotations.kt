// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

annotation class AnnParam

@setparam:AnnParam
var p: Int = 0

class C(@setparam:AnnParam var p: Int)
