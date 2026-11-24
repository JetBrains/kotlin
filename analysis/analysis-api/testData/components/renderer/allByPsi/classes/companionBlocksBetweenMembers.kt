// LANGUAGE: +CompanionBlocks
class StartsWithBlock {
    companion {
        fun first() {}
    }

    fun second() {}
}

class BlocksBetweenMembers {
    fun first() {}

    companion {
        fun second() {}

        val third: Int = 0
    }

    class Nested

    fun fourth() {}

    companion {
        fun fifth() {}
    }
}

enum class EnumWithBlocks {
    A, B;

    companion {
        fun first(): EnumWithBlocks = A
    }

    fun second() {}

    companion {
        fun third() {}
    }
}
