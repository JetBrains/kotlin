// LANGUAGE: +CompanionBlocks +CompanionExtensions
open class Owner {
    companion {
        protected fun <caret_target>target() {}
    }
}

class Sub : Owner() {
    fun test() {
        p<caret>rintln()
    }
}
