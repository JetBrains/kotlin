// I
// LANGUAGE: +CompanionBlocks
// LIBRARY_PLATFORMS: JVM
// JVM_DEFAULT_MODE: no-compatibility
// COMPILER_ARGUMENTS: -jvm-default=no-compatibility

interface I {
    companion {
        fun f1(x: String = "O") = x
        fun f2() = ""
        inline fun f3(x: String = "K") = x
    }
}
