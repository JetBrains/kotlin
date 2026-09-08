// LANGUAGE: +CompanionBlocks +CompanionExtensions
// DUMP_KLIB_ABI: DEFAULT
// WITH_STDLIB

class A {
    companion {
        val compBlockVal: UInt = 0u
        val compBlockValI: Int = 1
        inline fun compBlockFun(k: UInt = 0u) = " compBlockFun:$k"

        inline val compBlockInlineVal: String
            get() = " compBlockInlineVal:$compBlockValI"
        var compBlockVarI: Int = 0
        inline var compBlockInlineVar: String
            get() = " compBlockInlineVar:$compBlockVarI"
            set(value) {
                compBlockVarI = value.toInt()
            }
    }
}

fun box(): String {
    A.compBlockInlineVar = "42"
    val res = A.compBlockVal.toString() + A.compBlockValI.toString() + A.compBlockFun(5u) + A.compBlockInlineVal + A.compBlockInlineVar
    return if(res == "01 compBlockFun:5 compBlockInlineVal:1 compBlockInlineVar:42") "OK" else "FAIL: $res"
}
