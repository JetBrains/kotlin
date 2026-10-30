// JVM_DEFAULT_MODE: ENABLE
// LANGUAGE: +CompanionBlocks

annotation class Ann

interface A {
    @Ann val member: Int

    companion {
        @Ann val companion = 1
    }
}
