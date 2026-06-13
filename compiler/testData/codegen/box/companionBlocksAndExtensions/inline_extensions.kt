// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT

class A

companion val A.compExtVal: UInt = 3u
companion val A.compExtValI: Int = 4
companion inline fun A.compExtFun(k: UInt = 0u) = " compExtFun:$k"


fun box(): String {
    val res = A.compExtVal.toString() + A.compExtValI.toString() + A.compExtFun(6u)
    return if(res == "34 compExtFun:6") "OK" else "FAIL: $res"
}
