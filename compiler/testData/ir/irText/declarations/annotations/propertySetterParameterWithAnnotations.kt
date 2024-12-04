// FIR_IDENTICAL

annotation class AnnParam

@setparam:AnnParam
var p: Int = 0

class C(@setparam:AnnParam var p: Int)
