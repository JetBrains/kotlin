// I
// LANGUAGE: +CompanionBlocks
// JVM_DEFAULT_MODE: disable

interface I {
    private fun privateMember() {}

    fun publicMember() {}

    companion {
        private fun privateCompanionBlockMember() {}

        fun publicCompanionBlockMember() {}
    }
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: I.class[privateMember]
