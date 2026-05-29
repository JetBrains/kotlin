// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT
// IGNORE_BACKEND: WASM_JS, WASM_WASI
// Notes:
// WASM gets 0 from A.compBlockValI instead of 1


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
