// LANGUAGE: +CompanionBlocks +CompanionExtensions
// TARGET_BACKEND: JVM
// WITH_STDLIB
interface I {
    companion {
        const val O = "O"
    }
}

interface J {
    companion {
        @JvmField val K = "K"
    }
}

fun box() = I.O + J.K
