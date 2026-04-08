// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND_K1: ANY

class A {
    companion {
        val compBlockVal: String = "compBlockVal "
        fun compBlockFun(k: String = "") = "compBlockFun: $k "
    }
}

companion val A.compExtVal: String = "compExtVal "
companion fun A.compExtFun(k: String = "") = "compExtFun: $k "


fun box(): String { //todo: assertEquals instead of if &&
    return A.compBlockVal + A.compBlockFun("a") + A.compExtVal + A.compExtFun("b")
}
