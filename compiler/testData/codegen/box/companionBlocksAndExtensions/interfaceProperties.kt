// LANGUAGE: +CompanionBlocks +CompanionExtensions
// DONT_TARGET_EXACT_BACKEND: JVM_IR
// ^ interface properties on JVM must have @JvmField
// WITH_STDLIB
interface I {
    companion {
        const val O = "O"
        internal val K = "K"

        fun getOk() = O + K
    }
}

fun box() = I.getOk()
