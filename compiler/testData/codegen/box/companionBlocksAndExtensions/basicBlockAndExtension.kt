// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT
// DUMP_IR
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM, WASM_JS, WASM_WASI
// Notes:
// JS compBlockVal is undefined
// WASM dereferencing a null pointer

class A {
    companion {
        val compBlockVal: String = "blockVal "
        fun compBlockFun(k: String = "") = "blockFun:$k "
    }
}

companion val A.compExtVal: String = "extVal "
companion fun A.compExtFun(k: String = "") = "extFun:$k "


fun box(): String {
    val res = A.compBlockVal + A.compBlockFun() + A.compBlockFun("P") + A.compExtVal + A.compExtFun() + A.compExtFun("P")
    return if(res == "blockVal blockFun: blockFun:P extVal extFun: extFun:P ") "OK" else "FAIL: $res"
}
