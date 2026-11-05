// LANGUAGE: +CompanionBlocks +CompanionExtensions
open class Owner {
    companion {
        protected fun <caret_target>target() {}
    }
}

class Unrelated {
    fun test() {
        p<caret>rintln()
    }
}
