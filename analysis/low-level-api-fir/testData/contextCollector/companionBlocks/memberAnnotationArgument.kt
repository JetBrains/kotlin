// LANGUAGE: +CompanionBlocks +CompanionExtensions

annotation class Anno(val value: String)

class Owner {
    val memberProperty: String = "member"

    companion {
        const val COMPANION_CONSTANT = "companion"

        @Anno(<expr>COMPANION_CONSTANT</expr>)
        fun foo() {}
    }
}
