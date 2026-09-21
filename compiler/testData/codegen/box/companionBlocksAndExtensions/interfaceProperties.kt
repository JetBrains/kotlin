// LANGUAGE: +CompanionBlocks +CompanionExtensions

interface I {
    companion {
        const val O = "O"
        internal val K = "K"

        fun getOk() = O + K
    }
}

fun box() = I.getOk()
