// LANGUAGE: +CompanionBlocks
enum class E {
    A, B;

    fun regular() {}

    companion {
        fun fromName(name: String): E = valueOf(name)
    }
}

enum class OnlyEntriesAndCompanionBlock {
    A;

    companion {
        val default: OnlyEntriesAndCompanionBlock = A
    }
}
