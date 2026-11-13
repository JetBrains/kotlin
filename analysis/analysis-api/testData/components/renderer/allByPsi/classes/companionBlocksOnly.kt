// LANGUAGE: +CompanionBlocks
class OnlyCompanionBlocks {
    companion {
        fun foo() {}
    }

    companion {
        val bar: Int = 1
    }
}

class Outer {
    class Nested {
        fun regular() {}

        companion {
            fun static() {}
        }
    }
}
