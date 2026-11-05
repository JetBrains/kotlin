// LANGUAGE: +CompanionBlocks +CompanionExtensions
class Owner {
    companion {
        private fun <caret_target>target() {}
    }

    companion object {
        fun test() {
            p<caret>rintln()
        }
    }
}
