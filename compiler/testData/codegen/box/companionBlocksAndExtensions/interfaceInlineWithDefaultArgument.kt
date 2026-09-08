// LANGUAGE: +CompanionBlocks
// ISSUE: KT-89039

interface I {
    companion {
        inline fun foo(x: String = "OK") = x
    }
}

fun box() = I.foo()
