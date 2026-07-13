// LANGUAGE: +CompanionBlocksAndExtensions
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
interface I {
    companion {
        const val O = "O"
        @JvmField internal val K = "K"

        fun getOk() = O + K
    }
}

fun box() = I.getOk()
