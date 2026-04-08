// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND_K1: ANY
// WITH_SDLIB

class A {
    companion {
        val compBlockVal: String = "compBlockVal "
    }

    companion {
        fun compBlockFun(k: String = "") = "compBlockFun: $k "
    }
}

companion val A.compExtVal: String = "compExtValTopLevel"


Class B() {
    companion fun A.compExtFun(k: String = "") = "compExtFun: $k " // WRONG_MODIFIER_TARGET
    companion val A.compExtVal2
            get() = "compExtValInB"
    companion {
        fun foo() = A.compExtFun("foo") + A.compExtVal2
    }
}
fun box(): String { //todo: assertEquals instead of if &&
    return A.compBlockVal + A.compBlockFun("a") + A.compExtVal + A.compExtFun("b") + B.foo()
}
