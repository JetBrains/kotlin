// LANGUAGE: +CompanionBlocks +CompanionExtensions
open class Owner {
    companion {
        protected fun <caret_target>target() {}
    }
}

class Sub : Owner() {
    companion {
        fun test() {
            p<caret>rintln()
        }
    }
}
