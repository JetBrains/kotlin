// LANGUAGE: +CompanionBlocks +CompanionExtensions
class Owner {
    companion {
        private fun <caret_target>target() {}
    }

    fun test() {
        p<caret>rintln()
    }
}
