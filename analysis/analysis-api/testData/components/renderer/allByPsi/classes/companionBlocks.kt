// LANGUAGE: +CompanionBlocks +CompanionExtensions
class Owner {
    companion {
        fun foo(p: Int): Int = p
        val prop: Int = 1
        const val CONST: String = ""
        var mutableProp: Int
            get() = 1
            set(value) {}
        private fun privateFoo() {}
    }

    companion object {
        fun bar() {}
    }
}

companion fun Owner.ext(): Int = 1

companion val Owner.extProp: Int get() = 1
