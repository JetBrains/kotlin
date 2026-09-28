// I
// LANGUAGE: +CompanionBlocks
// JVM_DEFAULT_MODE: no-compatibility

interface I {
    companion {
        fun f1(x: String = "O") = x
        fun f2() = ""
        inline fun f3(x: String = "K") = x
    }
}
