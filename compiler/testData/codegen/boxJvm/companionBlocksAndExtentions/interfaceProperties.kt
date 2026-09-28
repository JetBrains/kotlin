// LANGUAGE: +CompanionBlocks +CompanionExtensions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// const and JvmField in one interface isn't supported, yet
// WITH_STDLIB
interface I {
    companion {
        const val O = "O"
        @JvmField val K = "K"

        fun getOk() = O + K
    }
}

fun box() = I.getOk()
