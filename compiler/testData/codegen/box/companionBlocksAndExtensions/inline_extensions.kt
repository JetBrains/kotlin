// LANGUAGE: +CompanionBlocks +CompanionExtensions
// DUMP_KLIB_ABI: DEFAULT
// WITH_STDLIB

class A

companion val A.compExtVal: UInt = 3u
companion val A.compExtValI: Int = 4
companion inline fun A.compExtFun(k: UInt = 0u) = " compExtFun:$k"

companion inline val A.compExtInlineVal: String
    get() = " compExtInlineVal:$compExtValI"
var compExtVarI: Int = 0
companion inline var A.compExtInlineVar: String
    get() = " compExtInlineVar:$compExtVarI"
    set(value) {
        compExtVarI = value.toInt()
    }

fun box(): String {
    A.compExtInlineVar = "42"
    val res = A.compExtVal.toString() + A.compExtValI.toString() + A.compExtFun(6u) + A.compExtInlineVal + A.compExtInlineVar
    return if(res == "34 compExtFun:6 compExtInlineVal:4 compExtInlineVar:42") "OK" else "FAIL: $res"
}
