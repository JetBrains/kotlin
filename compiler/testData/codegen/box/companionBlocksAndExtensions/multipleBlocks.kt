// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT

class A {
    companion {
        val compBlockVal: String = "K"
        fun compBlockFun(s: String) = compExtFun(s)
    }

    companion {
        fun compBlockFun2(s: String) = s
    }
}

companion fun A.compExtFun(s: String) = compBlockFun2(s + B.bar())
companion val A.compExtVal = "O"

class B() {
    companion {
        fun foo() = A.compBlockFun(A.compExtVal)
    }
    companion {
        fun bar() = A.compBlockVal
    }
}

fun box(): String {
    return B.foo()
}
