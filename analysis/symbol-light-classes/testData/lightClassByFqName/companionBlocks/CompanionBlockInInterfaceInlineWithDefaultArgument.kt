// I
// LANGUAGE: +CompanionBlocks

interface I {
    companion {
        inline fun foo(x: String = "OK") = x
    }
}
