// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND_K1: ANY
// WITH_SDLIB

class A {
    companion {
        val compBlockVal: UInt = 0u
        val compBlockValI: Int = 1
        inline fun compBlockFun(k: UInt = 0u) = " compBlockFun: $k "
    }
}

companion val A.compExtVal: UInt = 3u
companion val A.compExtValI: Int = 4
companion inline fun A.compExtFun(k: UInt = 0u) = " compExtFun: $k "


fun box(): String { //todo: assertEquals instead of if &&
    return A.compBlockVal.toString() + A.compBlockValI.toString() + A.compBlockFun(5u) + A.compExtVal.toString() + A.compExtValI.toString() + A.compExtFun(6u)
}
