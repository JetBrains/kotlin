// LANGUAGE: +CompanionBlocks
// JVM_DEFAULT_MODE: no-compatibility
// ISSUE: KT-89065

interface I {
    companion {
        fun f1(x: String = "O") = x
        fun f2() = ""
        inline fun f3(x: String = "K") = x
    }
}

fun box() = I.f1() + I.f2() + I.f3()
