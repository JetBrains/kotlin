// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT

class A {
    companion {
        val compBlockVal: UInt = 0u
        val compBlockValI: Int = 1
        inline fun compBlockFun(k: UInt = 0u) = " compBlockFun:$k"
    }
}

fun box(): String {
    val res = A.compBlockVal.toString() + A.compBlockValI.toString() + A.compBlockFun(5u)
    return if(res == "01 compBlockFun:5") "OK" else "FAIL: $res"
}
