// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT
// IGNORE_BACKEND: WASM_JS, WASM_WASI, JS_IR, JS_IR_ES6
// WITH_SDLIB
// Notes:
// WASM gets 0 from A.compBlockValI instead of 1
// JS fails because static vals are undefined


class A {
    companion {
        val compBlockVal: UInt = 0u
        val compBlockValI: Int = 1
        inline fun compBlockFun(k: UInt = 0u) = " compBlockFun:$k "
    }
}

companion val A.compExtVal: UInt = 3u
companion val A.compExtValI: Int = 4
companion inline fun A.compExtFun(k: UInt = 0u) = " compExtFun:$k"


fun box(): String {
    val res = A.compBlockVal.toString() + A.compBlockValI.toString() + A.compBlockFun(5u) + A.compExtVal.toString() + A.compExtValI.toString() + A.compExtFun(6u)
    return if(res == "01 compBlockFun:5 34 compExtFun:6") "OK" else "FAIL: $res"
}
